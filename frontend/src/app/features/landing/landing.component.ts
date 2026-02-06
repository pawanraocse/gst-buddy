import { Component, signal, HostListener, ViewChild, ElementRef, AfterViewInit, OnDestroy, QueryList, ViewChildren } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { AccordionModule } from 'primeng/accordion';
import { RippleModule } from 'primeng/ripple';
import { CarouselModule } from 'primeng/carousel';

@Component({
    selector: 'app-landing',
    standalone: true,
    imports: [CommonModule, RouterLink, FormsModule, ButtonModule, AccordionModule, RippleModule, CarouselModule],
    templateUrl: './landing.component.html',
    styleUrl: './landing.component.scss'
})
export class LandingComponent implements AfterViewInit, OnDestroy {
    @ViewChild('particleCanvas') particleCanvas!: ElementRef<HTMLCanvasElement>;
    @ViewChildren('animateOnScroll') animatedElements!: QueryList<ElementRef>;

    // UI State
    isScrolled = signal(false);
    mobileMenuOpen = signal(false);
    isLoading = signal(true);
    showRules = signal(false);
    activeStep = signal(1);
    timelineProgress = signal(0);

    // New: Video Modal
    showVideoModal = signal(false);

    // New: How-To Modal
    showHowToModal = signal(false);
    howToModalContent = signal<'tally' | 'busy' | null>(null);

    // New: Active section for navbar
    activeSection = signal('hero');

    // Typewriter
    typewriterText = '';
    private typewriterFullText = 'Handled by a Friend.';
    private typewriterIndex = 0;
    private typewriterInterval: ReturnType<typeof setInterval> | null = null;

    // 3D Tilt
    tiltX = 0;
    tiltY = 0;

    // Savings Card Animation
    savingsValue = signal(0);
    targetSavings = 124500;

    // Count-up animation
    animatedStats: number[] = [0, 0, 0, 0];
    private countUpStarted = false;

    // Particle system
    private particleCtx: CanvasRenderingContext2D | null = null;
    private particles: Particle[] = [];
    private animationFrameId: number | null = null;

    // FAQ Search
    faqSearchQuery = '';
    filteredFAQs: FAQ[] = [];

    // Pricing
    isYearly = signal(false);

    // ============================================
    // DATA
    // ============================================

    stats = [
        { value: 500, suffix: '+', icon: 'pi-users', label: 'Active Users' },
        { value: 50000, suffix: '+', icon: 'pi-file', label: 'Ledgers Processed' },
        { value: 99.9, suffix: '%', icon: 'pi-shield', label: 'Accuracy Rate' },
        { value: 24, suffix: '/7', icon: 'pi-clock', label: 'Support' }
    ];

    gstRules = [
        {
            image: 'assets/images/landing/icons/rule-37.png',
            rule: 'Rule 37',
            title: '180-Day ITC Reversal',
            description: 'Auto-calculate ITC reversals for unpaid invoices exceeding 180 days.',
            status: 'live'
        },
        {
            image: 'assets/images/landing/icons/rule-36-4.png',
            rule: 'Rule 36(4)',
            title: 'ITC Matching',
            description: 'Match claimed ITC with GSTR-2A/2B and identify mismatches instantly.',
            status: 'coming'
        },
        {
            image: 'assets/images/landing/icons/rule-86b.png',
            rule: 'Rule 86B',
            title: 'Credit Restriction',
            description: 'Monitor the 99% credit utilization threshold automatically.',
            status: 'coming'
        },
        {
            image: 'assets/images/landing/icons/rule-16-4.png',
            rule: 'Rule 16(4)',
            title: 'ITC Time Limit',
            description: 'Track ITC claims against due dates to avoid lapses.',
            status: 'coming'
        }
    ];

    features = [
        {
            image: 'assets/images/landing/features/smart-calc.png',
            icon: 'pi-bolt',
            title: 'Instant Calculation',
            description: 'Upload your ledger. Get Rule 37 results in seconds, not hours.',
            isLarge: true
        },
        {
            image: 'assets/images/landing/features/peace-of-mind.png',
            icon: 'pi-check-circle',
            title: 'Peace of Mind Dashboard',
            description: '"You are all clear" or "1 Action Pending" — at a glance.',
            isLarge: false
        },
        {
            image: 'assets/images/landing/features/security.png',
            icon: 'pi-lock',
            title: 'Bank-Grade Security',
            description: 'Your data is encrypted and never shared. AWS infrastructure.',
            isLarge: false
        },
        {
            image: 'assets/images/landing/features/auto-sync.png',
            icon: 'pi-cog',
            title: 'Always Updated',
            description: 'New GST rules added automatically as CBIC releases notifications.',
            isLarge: false
        }
    ];

    steps = [
        {
            number: 1,
            icon: 'pi-download',
            image: 'assets/images/landing/steps/step-1-export.png',
            title: 'Export Ledger',
            description: 'Export your party ledger from Tally or Busy. Takes 2 minutes.'
        },
        {
            number: 2,
            icon: 'pi-upload',
            image: 'assets/images/landing/steps/step-2-upload.png',
            title: 'Upload & Go',
            description: 'Drag & drop your Excel file. We do the rest in seconds.'
        },
        {
            number: 3,
            icon: 'pi-chart-bar',
            image: 'assets/images/landing/steps/step-3-results.png',
            title: 'Get Results',
            description: 'See ITC reversal, interest calculations, and export to Excel.'
        }
    ];

    testimonials = [
        {
            name: 'Rajesh Kumar',
            title: 'CA Partner',
            company: 'Kumar & Associates, Mumbai',
            quote: 'GST Buddy saved us 10+ hours every month. Our clients love the professional reports.',
            avatar: 'assets/images/landing/avatars/avatar-1.png',
            rating: 5
        },
        {
            name: 'Priya Sharma',
            title: 'Finance Head',
            company: 'Textile Exports Ltd, Surat',
            quote: 'Finally, no more Excel nightmares! The peace of mind dashboard is exactly what we needed.',
            avatar: 'assets/images/landing/avatars/avatar-2.png',
            rating: 5
        },
        {
            name: 'Amit Patel',
            title: 'Owner',
            company: 'Patel Trading Co, Ahmedabad',
            quote: 'Rule 37 compliance used to give me sleepless nights. Not anymore!',
            avatar: 'assets/images/landing/avatars/avatar-3.png',
            rating: 5
        }
    ];

    faqs: FAQ[] = [
        {
            question: 'What GST rules does GST Buddy support?',
            answer: 'We currently support <strong>Rule 37 (180-day ITC reversal)</strong> with Rule 36(4), 86B, 16(4), and GSTR-9 reconciliation coming soon. We add new rules automatically as CBIC releases notifications.',
            helpfulCount: 42
        },
        {
            question: 'Is GST Buddy free to try?',
            answer: 'Yes! Sign up for free and get <strong>5 full compliance checks</strong> at no cost — no credit card required. Try before you buy.',
            helpfulCount: 38
        },
        {
            question: 'What file formats do you support?',
            answer: 'Excel exports from <strong>Tally, Busy, or any party ledger</strong> in .xlsx/.xls format. We also support JSON exports from the GST Portal for GSTR-2A/2B matching.',
            helpfulCount: 31
        },
        {
            question: 'How is this different from other GST tools?',
            answer: 'Unlike fragmented tools that make you run separate checks for each rule, GST Buddy provides a <strong>unified dashboard</strong>. One upload, all rules checked. Plus our premium UI makes compliance actually pleasant.',
            helpfulCount: 56
        },
        {
            question: 'Is my data secure?',
            answer: 'Absolutely. We use <strong>bank-grade encryption (AES-256)</strong>, AWS infrastructure with SOC 2 compliance, and never share your data. Your files are processed and deleted — we don\'t store sensitive financial information.',
            helpfulCount: 67
        },
        {
            question: 'How do I export from Tally?',
            answer: 'It\'s simple! In Tally: Gateway → Display → Account Books → Ledger → Select Party → Export to Excel. We provide step-by-step guides with screenshots in our help center.',
            helpfulCount: 89
        },
        {
            question: 'Can I export reports for my clients?',
            answer: 'Yes! Export professional, <strong>white-labeled reports</strong> in Excel or PDF format — perfect for CA firms and tax consultants serving multiple clients.',
            helpfulCount: 45
        }
    ];

    pricingPlans = [
        {
            name: 'Starter',
            tagline: 'Perfect for trying out',
            price: 0,
            yearlyPrice: 0,
            features: [
                { text: '5 free calculations', included: true },
                { text: 'Rule 37 support', included: true },
                { text: 'Excel export', included: true },
                { text: 'Priority support', included: false }
            ],
            cta: 'Get Started Free',
            isPopular: false
        },
        {
            name: 'Professional',
            tagline: 'For growing businesses',
            price: 999,
            yearlyPrice: 799,
            features: [
                { text: 'Unlimited calculations', included: true },
                { text: 'All GST rules', included: true },
                { text: 'Priority email support', included: true },
                { text: 'Custom reports', included: true },
                { text: 'API access', included: true }
            ],
            cta: 'Start Free Trial',
            isPopular: true
        },
        {
            name: 'Enterprise',
            tagline: 'For CA firms & large teams',
            price: -1, // Custom
            yearlyPrice: -1,
            features: [
                { text: 'Everything in Professional', included: true },
                { text: 'Multi-user access', included: true },
                { text: 'Dedicated account manager', included: true },
                { text: 'White-label reports', included: true },
                { text: 'Custom integrations', included: true }
            ],
            cta: 'Contact Sales',
            isPopular: false
        }
    ];

    comparisons = [
        { feature: 'All Rules in One Place', us: true, others: false },
        { feature: 'Rule 37 (180-Day ITC Reversal)', us: true, others: true },
        { feature: 'Rule 36(4) ITC Matching', us: true, others: false },
        { feature: 'Rule 86B Credit Monitoring', us: true, others: false },
        { feature: 'GSTR-3B Compliance Check', us: true, others: false },
        { feature: 'Automatic Rule Updates', us: true, others: false },
        { feature: 'Zero Setup Required', us: true, others: false },
        { feature: 'Works with Free Tier', us: true, others: false },
        { feature: 'Professional Reports Export', us: true, others: true },
        { feature: 'Real-time Dashboard', us: true, others: false }
    ];

    recentActivities = [
        { name: 'Arun M.', action: 'signed up', timeAgo: '2 min ago', avatar: '🧑‍💼' },
        { name: 'Sunita K.', action: 'ran compliance check', timeAgo: '5 min ago', avatar: '👩‍💻' },
        { name: 'Vikram S.', action: 'exported report', timeAgo: '8 min ago', avatar: '👨‍💼' }
    ];

    trustedCompanies = [
        { name: 'Tally', logo: 'assets/images/landing/logos/tally.svg' },
        { name: 'Busy', logo: 'assets/images/landing/logos/busy.svg' }
    ];

    constructor() {
        this.filteredFAQs = [...this.faqs];
    }

    ngAfterViewInit(): void {
        // Hide preloader after 1.5s
        setTimeout(() => {
            this.isLoading.set(false);
            this.startTypewriter();
            this.animateSavings();
        }, 1500);

        // Initialize particle canvas
        this.initParticleCanvas();

        // Setup scroll observer for animations
        this.setupScrollObserver();

        // Start activity feed rotation
        this.rotateActivities();
    }

    animateSavings() {
        const duration = 2000; // 2 seconds
        const steps = 60;
        const stepValue = this.targetSavings / steps;
        let current = 0;

        const timer = setInterval(() => {
            current += stepValue;
            if (current >= this.targetSavings) {
                this.savingsValue.set(this.targetSavings);
                clearInterval(timer);
            } else {
                this.savingsValue.set(Math.floor(current));
            }
        }, duration / steps);
    }

    ngOnDestroy(): void {
        if (this.typewriterInterval) clearInterval(this.typewriterInterval);
        if (this.animationFrameId) cancelAnimationFrame(this.animationFrameId);
    }

    // ============================================
    // SCROLL & NAVIGATION
    // ============================================

    @HostListener('window:scroll')
    onScroll(): void {
        const scrollY = window.scrollY;
        this.isScrolled.set(scrollY > 50);

        // Parallax blob movement
        const blob1 = document.querySelector('.blob-1') as HTMLElement;
        const blob2 = document.querySelector('.blob-2') as HTMLElement;
        if (blob1 && blob2) {
            blob1.style.transform = `translate(${scrollY * 0.1}px, ${scrollY * 0.15}px)`;
            blob2.style.transform = `translate(${-scrollY * 0.08}px, ${scrollY * 0.12}px)`;
        }

        // Update timeline progress
        const timelineSection = document.getElementById('how-it-works');
        if (timelineSection) {
            const rect = timelineSection.getBoundingClientRect();
            const progress = Math.max(0, Math.min(100, ((window.innerHeight - rect.top) / (rect.height + window.innerHeight)) * 100));
            this.timelineProgress.set(progress);
        }

        // Trigger count-up when stats section is visible
        if (!this.countUpStarted) {
            const statsSection = document.getElementById('stats-bar');
            if (statsSection) {
                const rect = statsSection.getBoundingClientRect();
                if (rect.top < window.innerHeight * 0.8) {
                    this.countUpStarted = true;
                    this.animateCountUp();
                }
            }
        }

        // Active section tracking for navbar
        const sections = ['hero', 'features', 'how-it-works', 'pricing', 'faq'];
        for (const sectionId of sections) {
            const section = document.getElementById(sectionId);
            if (section) {
                const rect = section.getBoundingClientRect();
                if (rect.top <= 100 && rect.bottom >= 100) {
                    this.activeSection.set(sectionId);
                    break;
                }
            }
        }
    }

    @HostListener('window:keydown', ['$event'])
    onKeyDown(event: KeyboardEvent): void {
        if (event.key === 'Escape') {
            this.showVideoModal.set(false);
            this.showHowToModal.set(false);
            this.mobileMenuOpen.set(false);
        }
    }

    @HostListener('window:mousemove', ['$event'])
    onMouseMove(event: MouseEvent): void {
        const { clientX, clientY } = event;
        const { innerWidth, innerHeight } = window;

        // 3D Tilt effect for hero visual
        this.tiltX = ((clientY / innerHeight) - 0.5) * 15;
        this.tiltY = ((clientX / innerWidth) - 0.5) * -15;
    }

    toggleMobileMenu(): void {
        this.mobileMenuOpen.update(v => !v);
    }

    scrollTo(sectionId: string): void {
        this.mobileMenuOpen.set(false);
        const element = document.getElementById(sectionId);
        if (element) {
            const navbarHeight = 80;
            const elementPosition = element.getBoundingClientRect().top + window.scrollY;
            window.scrollTo({
                top: elementPosition - navbarHeight,
                behavior: 'smooth'
            });
        }
    }

    // ============================================
    // VIDEO MODAL
    // ============================================

    openVideoModal(): void {
        this.showVideoModal.set(true);
        document.body.style.overflow = 'hidden';
    }

    closeVideoModal(): void {
        this.showVideoModal.set(false);
        document.body.style.overflow = '';
    }

    // ============================================
    // HOW-TO MODAL
    // ============================================

    openHowToModal(type: 'tally' | 'busy'): void {
        this.howToModalContent.set(type);
        this.showHowToModal.set(true);
        document.body.style.overflow = 'hidden';
    }

    closeHowToModal(): void {
        this.showHowToModal.set(false);
        this.howToModalContent.set(null);
        document.body.style.overflow = '';
    }

    // ============================================
    // TYPEWRITER EFFECT
    // ============================================

    private startTypewriter(): void {
        this.typewriterInterval = setInterval(() => {
            if (this.typewriterIndex < this.typewriterFullText.length) {
                this.typewriterText += this.typewriterFullText.charAt(this.typewriterIndex);
                this.typewriterIndex++;
            } else {
                if (this.typewriterInterval) clearInterval(this.typewriterInterval);
            }
        }, 80);
    }

    // ============================================
    // MAGNETIC BUTTON EFFECT
    // ============================================

    magneticEffect(event: MouseEvent): void {
        const button = event.currentTarget as HTMLElement;
        const rect = button.getBoundingClientRect();
        const x = event.clientX - rect.left - rect.width / 2;
        const y = event.clientY - rect.top - rect.height / 2;

        button.style.transform = `translate(${x * 0.2}px, ${y * 0.2}px)`;
    }

    resetMagnetic(event: MouseEvent): void {
        const button = event.currentTarget as HTMLElement;
        button.style.transform = 'translate(0, 0)';
    }

    // ============================================
    // PARTICLE CANVAS
    // ============================================

    private initParticleCanvas(): void {
        if (!this.particleCanvas) return;

        const canvas = this.particleCanvas.nativeElement;
        this.particleCtx = canvas.getContext('2d');
        if (!this.particleCtx) return;

        canvas.width = window.innerWidth;
        canvas.height = window.innerHeight;

        // Create particles
        for (let i = 0; i < 50; i++) {
            this.particles.push(new Particle(canvas.width, canvas.height));
        }

        this.animateParticles();

        // Resize handler
        window.addEventListener('resize', () => {
            canvas.width = window.innerWidth;
            canvas.height = window.innerHeight;
        });
    }

    private animateParticles(): void {
        if (!this.particleCtx || !this.particleCanvas) return;

        const canvas = this.particleCanvas.nativeElement;
        const ctx = this.particleCtx;

        ctx.clearRect(0, 0, canvas.width, canvas.height);

        // Update and draw particles
        this.particles.forEach(p => {
            p.update(canvas.width, canvas.height);
            p.draw(ctx);
        });

        // Draw connections
        this.particles.forEach((p1, i) => {
            this.particles.slice(i + 1).forEach(p2 => {
                const dx = p1.x - p2.x;
                const dy = p1.y - p2.y;
                const distance = Math.sqrt(dx * dx + dy * dy);

                if (distance < 150) {
                    ctx.beginPath();
                    ctx.moveTo(p1.x, p1.y);
                    ctx.lineTo(p2.x, p2.y);
                    ctx.strokeStyle = `rgba(99, 102, 241, ${(1 - distance / 150) * 0.3})`;
                    ctx.lineWidth = 0.5;
                    ctx.stroke();
                }
            });
        });

        this.animationFrameId = requestAnimationFrame(() => this.animateParticles());
    }

    // ============================================
    // COUNT-UP ANIMATION
    // ============================================

    private animateCountUp(): void {
        const duration = 2000;
        const startTime = performance.now();

        const animate = (currentTime: number) => {
            const elapsed = currentTime - startTime;
            const progress = Math.min(elapsed / duration, 1);

            // Ease-out quad
            const easeProgress = 1 - (1 - progress) * (1 - progress);

            this.animatedStats = this.stats.map((stat, i) => {
                return Math.floor(stat.value * easeProgress);
            });

            if (progress < 1) {
                requestAnimationFrame(animate);
            } else {
                this.animatedStats = this.stats.map(s => s.value);
            }
        };

        requestAnimationFrame(animate);
    }

    // ============================================
    // SCROLL OBSERVER
    // ============================================

    private setupScrollObserver(): void {
        const observer = new IntersectionObserver(
            (entries) => {
                entries.forEach(entry => {
                    if (entry.isIntersecting) {
                        entry.target.classList.add('visible');
                    }
                });
            },
            { threshold: 0.1 }
        );

        // Observe all elements with animate-on-scroll class
        setTimeout(() => {
            document.querySelectorAll('.animate-on-scroll').forEach(el => {
                observer.observe(el);
            });
        }, 100);
    }

    // ============================================
    // FAQ SEARCH
    // ============================================

    filterFAQs(): void {
        const query = this.faqSearchQuery.toLowerCase().trim();
        if (!query) {
            this.filteredFAQs = [...this.faqs];
        } else {
            this.filteredFAQs = this.faqs.filter(faq =>
                faq.question.toLowerCase().includes(query) ||
                faq.answer.toLowerCase().includes(query)
            );
        }
    }

    markHelpful(index: number): void {
        if (this.filteredFAQs[index]) {
            this.filteredFAQs[index].helpfulCount++;
        }
    }

    // ============================================
    // PRICING
    // ============================================

    toggleBilling(): void {
        this.isYearly.update(v => !v);
    }

    getPrice(plan: typeof this.pricingPlans[0]): string {
        if (plan.price === -1) return 'Custom';
        return this.isYearly() ? plan.yearlyPrice.toString() : plan.price.toString();
    }

    // ============================================
    // TIMELINE
    // ============================================

    highlightStep(stepNumber: number): void {
        this.activeStep.set(stepNumber);
    }

    toggleRules(): void {
        this.showRules.update(v => !v);
    }

    // ============================================
    // ACTIVITY FEED
    // ============================================

    private rotateActivities(): void {
        setInterval(() => {
            const first = this.recentActivities.shift();
            if (first) {
                this.recentActivities.push(first);
            }
        }, 5000);
    }

    // ============================================
    // UTILITIES
    // ============================================

    get todayDate(): string {
        return new Date().toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
    }
}

// ============================================
// PARTICLE CLASS
// ============================================

class Particle {
    x: number;
    y: number;
    vx: number;
    vy: number;
    radius: number;

    constructor(canvasWidth: number, canvasHeight: number) {
        this.x = Math.random() * canvasWidth;
        this.y = Math.random() * canvasHeight;
        this.vx = (Math.random() - 0.5) * 0.5;
        this.vy = (Math.random() - 0.5) * 0.5;
        this.radius = Math.random() * 2 + 1;
    }

    update(canvasWidth: number, canvasHeight: number): void {
        this.x += this.vx;
        this.y += this.vy;

        if (this.x < 0 || this.x > canvasWidth) this.vx *= -1;
        if (this.y < 0 || this.y > canvasHeight) this.vy *= -1;
    }

    draw(ctx: CanvasRenderingContext2D): void {
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.radius, 0, Math.PI * 2);
        ctx.fillStyle = 'rgba(99, 102, 241, 0.4)';
        ctx.fill();
    }
}

// ============================================
// INTERFACES
// ============================================

interface FAQ {
    question: string;
    answer: string;
    helpfulCount: number;
}
