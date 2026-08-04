import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('life-comms-mock')
export class CommsMock extends LitElement {
  static override styles = css`
    :host {
      display: block;
      padding: var(--pages-space-3, 12px);
      overflow-y: auto;
      height: 100%;
      box-sizing: border-box;
    }

    h3 {
      margin: 0 0 var(--pages-space-3, 12px) 0;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }

    .message {
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-md, 6px);
      padding: var(--pages-space-3, 12px);
      margin-bottom: var(--pages-space-3, 12px);
      background: var(--pages-neutral-1, #fafafa);
    }

    .source {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 500;
      color: var(--pages-neutral-12, #111);
      margin-bottom: var(--pages-space-2, 8px);
    }

    .extraction {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      padding: var(--pages-space-1, 4px) 0;
      padding-left: var(--pages-space-4, 16px);
    }

    .arrow {
      color: var(--pages-green-9, #16a34a);
    }

    .coming-soon {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-7, #a3a3a3);
      font-style: italic;
      text-align: center;
      padding: var(--pages-space-3, 12px);
      border-top: 1px solid var(--pages-neutral-3, #f0f0f0);
      margin-top: var(--pages-space-3, 12px);
    }
  `;

  override render() {
    return html`
      <h3>Recent Activity</h3>

      <div class="message">
        <div class="source">📧 School: Trip consent form</div>
        <div class="extraction"><span class="arrow">→</span> extracted: deadline 15 Aug</div>
        <div class="extraction"><span class="arrow">→</span> created: task for Mark</div>
      </div>

      <div class="message">
        <div class="source">💬 Bob's Plumbing: "Thursday 2pm"</div>
        <div class="extraction"><span class="arrow">→</span> extracted: appointment</div>
        <div class="extraction"><span class="arrow">→</span> updated: contractor case</div>
        <div class="extraction"><span class="arrow">→</span> trust: confirmed commitment</div>
      </div>

      <div class="message">
        <div class="source">📧 Solicitor: "Respond by Aug 30"</div>
        <div class="extraction"><span class="arrow">→</span> extracted: legal deadline</div>
        <div class="extraction"><span class="arrow">→</span> created: SLA-tracked task</div>
      </div>

      <p class="coming-soon">Coming soon — Email & WhatsApp integration</p>
    `;
  }
}
