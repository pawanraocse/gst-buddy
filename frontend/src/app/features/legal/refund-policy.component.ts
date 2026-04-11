import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-refund-policy',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="legal-page">
      <div class="legal-container">
        <a routerLink="/" class="back-link">
          <span>←</span> Back to Home
        </a>

        <div class="legal-header">
          <div class="legal-icon">💳</div>
          <h1>Refund Policy</h1>
          <p class="last-updated">Last updated: March 2026</p>
        </div>

        <div class="legal-body">
          <section>
            <h2>Our Commitment</h2>
            <p>We want you to be completely satisfied with Gstbuddies. Our credit-based model means you only pay for what you use — here's how refunds work.</p>
          </section>

          <section>
            <h2>Credit Refunds</h2>
            <p>Credits are eligible for a full refund in the following cases:</p>
            <ul>
              <li><strong>Platform Error:</strong> If a compliance analysis fails due to a bug or platform issue on our end, the credit is automatically restored to your account.</li>
              <li><strong>Duplicate Purchase:</strong> If you accidentally purchase the same credit pack twice within 24 hours, contact us and we'll refund the duplicate.</li>
              <li><strong>Unused Credits within 7 days:</strong> If you purchased credits and haven't used any of them, you may request a full refund within 7 days of purchase.</li>
            </ul>
          </section>

          <section>
            <h2>Non-Refundable Cases</h2>
            <ul>
              <li>Credits that have already been used for an analysis (regardless of the result)</li>
              <li>Credits purchased more than 7 days ago that have not been used</li>
              <li>Cases where you uploaded an incorrect file format</li>
            </ul>
          </section>

          <section>
            <h2>How to Request a Refund</h2>
            <p>To request a refund:</p>
            <ol>
              <li>Email <a href="mailto:support@gstbuddies.com">support@gstbuddies.com</a> with subject: "Refund Request"</li>
              <li>Include your registered email address and order ID (from payment confirmation)</li>
              <li>Describe the reason for your refund</li>
            </ol>
            <p>We process refunds within <strong>3–5 business days</strong>. Refunds are credited back to the original payment method via Razorpay.</p>
          </section>

          <section>
            <h2>Contact</h2>
            <p>For refund-related queries, please contact us at <a href="mailto:support@gstbuddies.com">support@gstbuddies.com</a>.</p>
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
    .legal-header { text-align: center; margin-bottom: 48px; }
    .legal-icon { font-size: 3rem; margin-bottom: 16px; }
    h1 { font-size: 2.5rem; color: #fff; font-weight: 700; margin: 0 0 12px; }
    .last-updated { color: #6b7280; font-size: 0.9rem; }
    .legal-body section {
      margin-bottom: 40px;
      padding-bottom: 40px;
      border-bottom: 1px solid rgba(255,255,255,0.08);
    }
    .legal-body section:last-child { border-bottom: none; }
    h2 { font-size: 1.25rem; color: #e2e8f0; font-weight: 600; margin: 0 0 16px; }
    p, li { color: #9ca3af; line-height: 1.8; font-size: 0.95rem; }
    ul, ol { padding-left: 20px; margin: 12px 0; }
    li { margin-bottom: 8px; }
    strong { color: #e2e8f0; }
    a { color: #8b5cf6; text-decoration: none; }
    a:hover { text-decoration: underline; }
  `]
})
export class RefundPolicyComponent {}
