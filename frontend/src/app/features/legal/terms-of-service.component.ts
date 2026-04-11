import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-terms-of-service',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="legal-page">
      <div class="legal-container">
        <a routerLink="/" class="back-link">
          <span>←</span> Back to Home
        </a>

        <div class="legal-header">
          <div class="legal-icon">📋</div>
          <h1>Terms of Service</h1>
          <p class="last-updated">Last updated: March 2026</p>
        </div>

        <div class="legal-body">
          <section>
            <h2>1. Acceptance of Terms</h2>
            <p>By accessing or using GSTBuddies ("the Service"), you agree to be bound by these Terms of Service. If you do not agree, please do not use the Service.</p>
          </section>

          <section>
            <h2>2. Description of Service</h2>
            <p>GSTBuddies provides automated GST compliance tools for Indian businesses, including but not limited to Rule 37 (180-day ITC reversal) calculations, ledger analysis, and compliance reporting ("the Service").</p>
            <p>The Service is intended as a <strong>decision-support tool</strong>. Output should be reviewed by a qualified Chartered Accountant before filing any GST return.</p>
          </section>

          <section>
            <h2>3. User Accounts</h2>
            <ul>
              <li>You must provide accurate registration information</li>
              <li>You are responsible for maintaining the security of your account credentials</li>
              <li>You must not share your account with others</li>
              <li>You must notify us immediately of any unauthorized access</li>
            </ul>
          </section>

          <section>
            <h2>4. Credits and Payments</h2>
            <ul>
              <li>Credits are required to perform compliance analyses</li>
              <li>Credits are non-transferable and non-refundable unless the analysis failed due to a platform error</li>
              <li>Payments are processed by Razorpay and subject to their terms</li>
              <li>Prices are in INR and inclusive of applicable taxes</li>
            </ul>
          </section>

          <section>
            <h2>5. Acceptable Use</h2>
            <p>You agree not to:</p>
            <ul>
              <li>Upload files containing malicious code</li>
              <li>Attempt to reverse-engineer or scrape the Service</li>
              <li>Use the Service for any unlawful purpose</li>
              <li>Impersonate another user or entity</li>
            </ul>
          </section>

          <section>
            <h2>6. Disclaimer of Warranties</h2>
            <p>The Service is provided "as is." While we strive for 99.9% accuracy, GSTBuddies does not warrant that calculations are free from error or that the Service will be uninterrupted. Tax compliance decisions should always be verified with a qualified CA.</p>
          </section>

          <section>
            <h2>7. Limitation of Liability</h2>
            <p>To the maximum extent permitted by law, GSTBuddies shall not be liable for any indirect, incidental, or consequential damages arising from your use of the Service.</p>
          </section>

          <section>
            <h2>8. Governing Law</h2>
            <p>These Terms are governed by the laws of India. Any disputes shall be subject to the exclusive jurisdiction of the courts in Bangalore, Karnataka.</p>
          </section>

          <section>
            <h2>9. Contact</h2>
            <p>For questions about these Terms, contact us at <a href="mailto:support@gstbuddies.com">support@gstbuddies.com</a>.</p>
          </section>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .legal-page {
      min-height: 100vh;
      background: #0a0a1a;
      padding: 40px 20px 80px;
      font-family: 'Inter', sans-serif;
    }
    .legal-container {
      max-width: 760px;
      margin: 0 auto;
    }
    .back-link {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      color: #8b5cf6;
      text-decoration: none;
      font-size: 0.9rem;
      margin-bottom: 40px;
      transition: opacity 0.2s;
    }
    .back-link:hover { opacity: 0.7; }
    .legal-header {
      text-align: center;
      margin-bottom: 48px;
    }
    .legal-icon { font-size: 3rem; margin-bottom: 16px; }
    h1 {
      font-size: 2.5rem;
      color: #fff;
      font-weight: 700;
      margin: 0 0 12px;
    }
    .last-updated { color: #6b7280; font-size: 0.9rem; }
    .legal-body section {
      margin-bottom: 40px;
      padding-bottom: 40px;
      border-bottom: 1px solid rgba(255,255,255,0.08);
    }
    .legal-body section:last-child { border-bottom: none; }
    h2 { font-size: 1.25rem; color: #e2e8f0; font-weight: 600; margin: 0 0 16px; }
    p, li { color: #9ca3af; line-height: 1.8; font-size: 0.95rem; }
    ul { padding-left: 20px; margin: 12px 0; }
    li { margin-bottom: 8px; }
    strong { color: #e2e8f0; }
    a { color: #8b5cf6; text-decoration: none; }
    a:hover { text-decoration: underline; }
  `]
})
export class TermsOfServiceComponent {}
