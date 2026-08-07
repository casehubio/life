import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { LifeEventController, CASE_EVENT_TYPES } from '../events/life-event-controller.js';

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

const DOMAIN_LABELS: Record<string, string> = {
  HOUSEHOLD: 'Household',
  HEALTH: 'Health',
  FINANCE: 'Finance',
  FAMILY_SCHEDULING: 'Family',
  TRAVEL: 'Travel',
  LEGAL: 'Legal',
  CONTRACTOR_COORDINATION: 'Contractor',
  ELDER_CARE: 'Elder Care',
};

const CASE_LABELS: Record<string, string> = {
  CONTRACTOR_COORDINATION: 'Contractor Coordination',
  CARE_COORDINATION: 'Care Coordination',
  TRAVEL_PLAN: 'Travel Plan',
  APPOINTMENT_CYCLE: 'Appointment Cycle',
  FINANCIAL_REVIEW: 'Financial Review',
  HOME_MAINTENANCE: 'Home Maintenance',
};

interface DomainGroup {
  domain: string;
  cases: LifeCase[];
}

@customElement('life-active-cases')
export class ActiveCases extends LitElement {
  private _events = new LifeEventController(this, {
    types: CASE_EVENT_TYPES,
    onEvent: () => this._fetch(),
  });

  static override styles = css`
    :host { display: block; }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--pages-space-3, 12px);
    }

    h3 {
      margin: 0;
      font-size: var(--pages-font-size-base, 15px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }

    .count {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }

    .domain-group {
      margin-bottom: var(--pages-space-4, 16px);
    }

    .domain-header {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 500;
      color: var(--pages-neutral-11, #262626);
      margin-bottom: var(--pages-space-2, 8px);
      padding-bottom: var(--pages-space-1, 4px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }

    .case-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      border-radius: var(--pages-radius-md, 6px);
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-11, #262626);
      cursor: pointer;
      transition: background var(--pages-duration-fast, 120ms);
    }

    .case-item:hover { background: var(--pages-neutral-3, #f0f0f0); }

    .case-name { font-weight: 400; }

    .case-age {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
    }

    .empty, .loading {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-8, #737373);
    }
  `;

  @state() private _groups: DomainGroup[] = [];
  @state() private _totalCount = 0;
  @state() private _loading = true;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const res = await fetch('/life-cases?status=ACTIVE&size=50');
      if (!res.ok) return;
      const data = await res.json();
      const cases: LifeCase[] = data.items;
      this._totalCount = cases.length;

      const grouped = new Map<string, LifeCase[]>();
      for (const c of cases) {
        const list = grouped.get(c.domain) ?? [];
        list.push(c);
        grouped.set(c.domain, list);
      }
      this._groups = [...grouped.entries()].map(([domain, items]) => ({ domain, cases: items }));
    } catch (e) { console.error(e); }
    finally { this._loading = false; }
  }

  private _formatAge(iso: string): string {
    const days = Math.round((Date.now() - new Date(iso).getTime()) / 86_400_000);
    if (days === 0) return 'today';
    if (days === 1) return '1 day ago';
    return `${days}d ago`;
  }

  override render() {
    if (this._loading) return html`<p class="loading">Loading cases…</p>`;

    return html`
      <div class="header">
        <h3>Active Cases by Domain</h3>
        ${this._totalCount > 0 ? html`<span class="count">${this._totalCount} active</span>` : ''}
      </div>
      ${this._groups.length > 0
        ? this._groups.map(g => html`
          <div class="domain-group">
            <div class="domain-header">
              <span>${DOMAIN_ICONS[g.domain] ?? '\u{1F4CB}'}</span>
              <span>${DOMAIN_LABELS[g.domain] ?? g.domain}</span>
            </div>
            ${g.cases.map(c => html`
              <div class="case-item" @click=${() => { window.location.hash = 'cases'; }}>
                <span class="case-name">${CASE_LABELS[c.caseType] ?? c.caseType}</span>
                <span class="case-age">${this._formatAge(c.createdAt)}</span>
              </div>
            `)}
          </div>
        `)
        : html`<p class="empty">No active cases</p>`}
    `;
  }
}
