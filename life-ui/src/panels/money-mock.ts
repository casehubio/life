import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('life-money-mock')
export class MoneyMock extends LitElement {
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

    .section {
      margin-bottom: var(--pages-space-4, 16px);
    }

    .total {
      font-size: var(--pages-font-size-lg, 16px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
      margin-bottom: var(--pages-space-2, 8px);
    }

    .breakdown {
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-1, 4px);
      padding-left: var(--pages-space-3, 12px);
    }

    .breakdown-item {
      display: flex;
      justify-content: space-between;
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
    }

    .bill {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-11, #262626);
      padding: var(--pages-space-1, 4px) 0;
    }

    .bill-date {
      color: var(--pages-neutral-8, #737373);
      min-width: 32px;
    }

    .alert {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-amber-11, #92400e);
      padding: var(--pages-space-2, 8px);
      background: var(--pages-amber-2, #fffbeb);
      border-radius: var(--pages-radius-sm, 4px);
    }

    .coming-soon {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-7, #a3a3a3);
      font-style: italic;
      text-align: center;
      padding: var(--pages-space-3, 12px);
      border-top: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
  `;

  override render() {
    return html`
      <h3>This Month</h3>

      <div class="section">
        <div class="total">Total spend: £3,240</div>
        <div class="breakdown">
          <div class="breakdown-item"><span>Household</span><span>£1,890</span></div>
          <div class="breakdown-item"><span>Ella (school, swimming)</span><span>£420</span></div>
          <div class="breakdown-item"><span>Tom (school, football)</span><span>£380</span></div>
          <div class="breakdown-item"><span>Other</span><span>£550</span></div>
        </div>
      </div>

      <div class="section">
        <h3>Bills Due</h3>
        <div class="bill"><span class="bill-date">5th</span><span>Council tax</span><span>£180</span></div>
        <div class="bill"><span class="bill-date">12th</span><span>Electricity</span><span>£95</span></div>
        <div class="bill"><span class="bill-date">15th</span><span>Broadband</span><span>£45</span></div>
      </div>

      <div class="alert">⚠ Netflix increased £2/mo</div>

      <p class="coming-soon">Coming soon — Open Banking integration</p>
    `;
  }
}
