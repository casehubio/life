import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface LifeCase {
  id: string;
  caseType: string;
  domain: string;
  status: string;
  createdAt: string;
  completedAt: string | null;
}

const DOMAIN_ICONS: Record<string, string> = {
  HOUSEHOLD: '\u{1F3E0}', HEALTH: '\u{1F3E5}', FINANCE: '\u{1F4B0}',
  FAMILY_SCHEDULING: '\u{1F4C5}', TRAVEL: '✈️', LEGAL: '⚖️',
  CONTRACTOR_COORDINATION: '\u{1F527}', ELDER_CARE: '❤️',
};

const CASE_LABELS: Record<string, string> = {
  CONTRACTOR_COORDINATION: 'Contractor Coordination',
  CARE_COORDINATION: 'Care Coordination',
  TRAVEL_PLAN: 'Travel Plan',
  APPOINTMENT_CYCLE: 'Appointment Cycle',
  FINANCIAL_REVIEW: 'Financial Review',
  HOME_MAINTENANCE: 'Home Maintenance',
};

const STATUS_COLORS: Record<string, string> = {
  ACTIVE: 'var(--pages-green-9, #16a34a)',
  COMPLETED: 'var(--pages-neutral-8, #737373)',
  FAILED: 'var(--pages-red-9, #dc2626)',
};

@customElement('cases-view')
export class CasesView extends LitElement {
  static override styles = css`
    :host { display: flex; height: 100%; gap: 1px; background: var(--pages-neutral-4, #d4d4d4); }
    .list-pane {
      width: 360px; flex-shrink: 0;
      background: var(--pages-neutral-1, #fafafa);
      display: flex; flex-direction: column; overflow: hidden;
    }
    .filters {
      display: flex; gap: var(--pages-space-2, 8px);
      padding: var(--pages-space-3, 12px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .filter-btn {
      padding: var(--pages-space-1, 4px) var(--pages-space-3, 12px);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-md, 6px);
      background: none; cursor: pointer;
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      transition: all var(--pages-duration-fast, 120ms);
    }
    .filter-btn:hover { background: var(--pages-neutral-3, #f0f0f0); }
    .filter-btn[data-active] {
      background: var(--pages-accent-3, #e0e7ff);
      border-color: var(--pages-accent-7, #818cf8);
      color: var(--pages-accent-11, #3730a3);
    }
    .case-list { flex: 1; overflow-y: auto; }
    .case-item {
      display: flex; align-items: center; gap: var(--pages-space-3, 12px);
      padding: var(--pages-space-3, 12px) var(--pages-space-4, 16px);
      cursor: pointer;
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
      transition: background var(--pages-duration-fast, 120ms);
    }
    .case-item:hover { background: var(--pages-neutral-2, #f5f5f5); }
    .case-item[data-selected] { background: var(--pages-accent-3, #e0e7ff); }
    .case-icon { font-size: var(--pages-font-size-lg, 16px); flex-shrink: 0; }
    .case-info { flex: 1; min-width: 0; }
    .case-name {
      font-size: var(--pages-font-size-sm, 14px); font-weight: 500;
      color: var(--pages-neutral-12, #111);
    }
    .case-meta {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      display: flex; gap: var(--pages-space-2, 8px);
    }
    .status-dot {
      display: inline-block; width: 8px; height: 8px;
      border-radius: 50%; flex-shrink: 0; margin-top: 3px;
    }
    .detail-pane {
      flex: 1; background: var(--pages-neutral-1, #fafafa);
      overflow-y: auto; padding: var(--pages-space-5, 20px);
    }
    .detail-header {
      margin-bottom: var(--pages-space-5, 20px);
      padding-bottom: var(--pages-space-4, 16px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .detail-title {
      font-size: var(--pages-font-size-xl, 20px); font-weight: 600;
      color: var(--pages-neutral-12, #111);
      display: flex; align-items: center; gap: var(--pages-space-3, 12px);
    }
    .detail-subtitle {
      font-size: var(--pages-font-size-sm, 14px);
      color: var(--pages-neutral-8, #737373);
      margin-top: var(--pages-space-1, 4px);
    }
    .field {
      display: flex; justify-content: space-between;
      padding: var(--pages-space-2, 8px) 0;
      font-size: var(--pages-font-size-sm, 14px);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    .field-label { color: var(--pages-neutral-8, #737373); }
    .field-value { color: var(--pages-neutral-12, #111); font-weight: 500; }
    .empty-detail {
      display: flex; align-items: center; justify-content: center; height: 100%;
      color: var(--pages-neutral-8, #737373); font-size: var(--pages-font-size-base, 15px);
    }
  `;

  @state() private _cases: LifeCase[] = [];
  @state() private _filtered: LifeCase[] = [];
  @state() private _selected: LifeCase | null = null;
  @state() private _statusFilter: string | null = null;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const res = await fetch('/life-cases?size=50');
      if (!res.ok) return;
      const data = await res.json();
      this._cases = data.items;
      this._applyFilter();
      if (this._filtered.length > 0) this._selected = this._filtered[0];
    } catch { /* empty */ }
  }

  private _applyFilter(): void {
    this._filtered = this._statusFilter
      ? this._cases.filter(c => c.status === this._statusFilter)
      : this._cases;
  }

  private _setFilter(status: string | null): void {
    this._statusFilter = this._statusFilter === status ? null : status;
    this._applyFilter();
  }

  private _formatDate(iso: string): string {
    return new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  private _formatAge(iso: string): string {
    const days = Math.round((Date.now() - new Date(iso).getTime()) / 86_400_000);
    if (days === 0) return 'today';
    if (days === 1) return '1 day';
    return `${days} days`;
  }

  override render() {
    return html`
      <div class="list-pane">
        <div class="filters">
          ${['ACTIVE', 'COMPLETED', 'FAILED'].map(s => html`
            <button class="filter-btn" ?data-active=${this._statusFilter === s}
                    @click=${() => this._setFilter(s)}>${s.toLowerCase()}</button>
          `)}
        </div>
        <div class="case-list">
          ${this._filtered.map(c => html`
            <div class="case-item" ?data-selected=${this._selected?.id === c.id}
                 @click=${() => { this._selected = c; }}>
              <span class="case-icon">${DOMAIN_ICONS[c.domain] ?? '\u{1F4CB}'}</span>
              <div class="case-info">
                <div class="case-name">${CASE_LABELS[c.caseType] ?? c.caseType}</div>
                <div class="case-meta">
                  <span class="status-dot" style="background: ${STATUS_COLORS[c.status] ?? '#999'}"></span>
                  <span>${c.status.toLowerCase()}</span>
                  <span>${this._formatAge(c.createdAt)}</span>
                </div>
              </div>
            </div>
          `)}
        </div>
      </div>
      <div class="detail-pane">
        ${this._selected ? this._renderDetail(this._selected) : html`<div class="empty-detail">Select a case</div>`}
      </div>
    `;
  }

  private _renderDetail(c: LifeCase) {
    return html`
      <div class="detail-header">
        <div class="detail-title">
          <span>${DOMAIN_ICONS[c.domain] ?? '\u{1F4CB}'}</span>
          ${CASE_LABELS[c.caseType] ?? c.caseType}
        </div>
        <div class="detail-subtitle">${c.domain.replace(/_/g, ' ')} · ${c.status.toLowerCase()}</div>
      </div>
      <div class="field"><span class="field-label">Status</span><span class="field-value">${c.status}</span></div>
      <div class="field"><span class="field-label">Domain</span><span class="field-value">${c.domain.replace(/_/g, ' ')}</span></div>
      <div class="field"><span class="field-label">Started</span><span class="field-value">${this._formatDate(c.createdAt)}</span></div>
      ${c.completedAt ? html`<div class="field"><span class="field-label">Completed</span><span class="field-value">${this._formatDate(c.completedAt)}</span></div>` : ''}
      <div class="field"><span class="field-label">Duration</span><span class="field-value">${this._formatAge(c.createdAt)}</span></div>
      <p style="color: var(--pages-neutral-8, #737373); font-size: 14px; margin-top: 24px;">
        Timeline, workers, routing, audit trail, and CBR tabs coming in next iteration.
      </p>
    `;
  }
}
