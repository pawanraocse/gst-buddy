import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="legal-page">
      <div class="legal-container">
        <a routerLink="/" class="back-link">
          <span>←</span> Back to Home
        </a>

        <div class="legal-header">
          <div class="legal-icon">🔒</div>
          <h1>Privacy Policy</h1>
          <p class="last-updated">Last updated: March 2026</p>
        </div>

        <div class="legal-body">
          <section>
            <h2>1. Information We Collect</h2>
            <p>When you use Gstbuddies, we collect the following information to provide our GST compliance services:</p>
            <ul>
              <li><strong>Account Information:</strong> Name, email address, and GSTIN (if provided).</li>
              <li><strong>Uploaded Files:</strong> Excel ledger files you upload for Rule 37 and other GST compliance checks.</li>
              <li><strong>Usage Data:</strong> How you interact with our platform (pages visited, features used, timestamps).</li>
              <li><strong>Payment Information:</strong> Processed via Razorpay; we do not store card details on our servers.</li>
            </ul>
          </section>

          <section>
            <h2>2. How We Use Your Information</h2>
            <p>We use your information exclusively to:</p>
            <ul>
              <li>Provide GST compliance calculations and reports</li>
              <li>Send transactional emails (verification, payment receipts)</li>
              <li>Improve our platform based on usage patterns</li>
              <li>Respond to support queries</li>
            </ul>
            <p>We <strong>do not sell, rent, or share</strong> your personal data with third parties for marketing purposes.</p>
          </section>

          <section>
            <h2>3. Data Storage and Security</h2>
            <p>Your data is stored on AWS infrastructure (ap-south-1 region) with:</p>
            <ul>
              <li>AES-256 encryption at rest</li>
              <li>TLS 1.2+ encryption in transit</li>
              <li>Uploaded ledger files are processed and automatically deleted after 7 days</li>
              <li>Access is restricted to authenticated users only</li>
            </ul>
          </section>

          <section>
            <h2>4. Your Rights</h2>
            <p>You have the right to:</p>
            <ul>
              <li>Access the personal data we hold about you</li>
              <li>Request deletion of your account and all associated data</li>
              <li>Export your compliance reports at any time</li>
            </ul>
            <p>To exercise these rights, contact us at <a href="mailto:support@gstbuddies.com">support@gstbuddies.com</a>.</p>
          </section>

          <section>
            <h2>5. Cookies</h2>
            <p>We use strictly necessary cookies for authentication sessions. We do not use tracking or advertising cookies.</p>
          </section>

          <section>
            <h2>6. Changes to This Policy</h2>
            <p>We may update this policy from time to time. We will notify you of significant changes via email.</p>
          </section>

          <section>
            <h2>7. Contact</h2>
            <p>For privacy-related questions, please contact us at <a href="mailto:support@gstbuddies.com">support@gstbuddies.com</a>.</p>
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
    .legal-icon {
      font-size: 3rem;
      margin-bottom: 16px;
    }
    h1 {
      font-size: 2.5rem;
      color: #fff;
      font-weight: 700;
      margin: 0 0 12px;
    }
    .last-updated {
      color: #6b7280;
      font-size: 0.9rem;
    }
    .legal-body section {
      margin-bottom: 40px;
      padding-bottom: 40px;
      border-bottom: 1px solid rgba(255,255,255,0.08);
    }
    .legal-body section:last-child { border-bottom: none; }
    h2 {
      font-size: 1.25rem;
      color: #e2e8f0;
      font-weight: 600;
      margin: 0 0 16px;
    }
    p, li {
      color: #9ca3af;
      line-height: 1.8;
      font-size: 0.95rem;
    }
    ul { padding-left: 20px; margin: 12px 0; }
    li { margin-bottom: 8px; }
    strong { color: #e2e8f0; }
    a { color: #8b5cf6; text-decoration: none; }
    a:hover { text-decoration: underline; }
  `]
})
export class PrivacyPolicyComponent {}
