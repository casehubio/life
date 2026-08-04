import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface LifeCase {
  id: string;
  caseType: string;
  domain: string;
  status: string;
  createdAt: string;
}

const DOMAIN_ICONS: Record<string, string> = {
  HOUSEHOLD: '\u{1F3E0}',
  HEALTH: '\u{1F3E5}',
  FINANCE: '\u{1F4B0}',
  FAMILY_SCHEDULING: '\u{1F4C5}',
  TRAVEL: '✈️',
  LEGAL: '⚖️',
  CONTRACTOR_COORDINATION: '\u{1F527}',
  ELDER_CARE: '❤️',
};

const CASE_LABELS: Record<string, string> = {
  CONTRACTOR_COORDINATION: 'Contractor',
  CARE_COORDINATION: 'Care',
  TRAVEL_PLAN: 'Travel',
  APPOINTMENT_CYCLE: 'Appointment',
  FINANCIAL_REVIEW: 'Finance',
  HOME_MAINTENANCE: 'Home',
};

@customElement('life-cases-dock')
export class CasesDock extends LitElement {
  static override styles = css`
    :host {
      display: block;
      height: 100%;
      overflow-y: auto;
      padding: var(--pages-space-3, 12px);
      box-sizing: border-box;
    }
    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--pages-space-3, 12px);
    }
    h3 {
      margin: 0;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }
    .count {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }
    .items {
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-1, 4px);
    }
    .item {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      padding: var(--pages-space-2, 8px);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-11, #262626);
      cursor: pointer;
      transition: background var(--pages-duration-fast, 120ms);
    }
    .item:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .icon { flex-shrink: 0; }
    .label { flex: 1; }
    .domain {
      font-size: 10px;
      color: var(--pages-neutral-8, #737373);
    }
    .empty, .loading {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }
  `;

  @state() private _cases: LifeCase[] = [];
  @state() private _loading = true;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const res = await fetch('/life-cases?status=ACTIVE&size=10');
      if (!res.ok) return;
      const data = await res.json();
      this._cases = data.items;
    } catch { /* empty */ }
    finally { this._loading = false; }
  }

  override render() {
    if (this._loading) return html`<p class="loading">Loading…</p>`;
    return html`
      <div class="header">
        <h3>Active Cases</h3>
        ${this._cases.length > 0 ? html`<span class="count">${this._cases.length}</span>` : ''}
      </div>
      ${this._cases.length > 0
        ? html`<div class="items">${this._cases.map(c => html`
            <div class="item" @click=${() => { window.location.hash = 'cases'; }}>
              <span class="icon">${DOMAIN_ICONS[c.domain] ?? '\u{1F4CB}'}</span>
              <span class="label">${CASE_LABELS[c.caseType] ?? c.caseType}</span>
              <span class="domain">${c.domain.replace(/_/g, ' ').toLowerCase()}</span>
            </div>
          `)}</div>`
        : html`<p class="empty">No active cases</p>`}
    `;
  }
}
