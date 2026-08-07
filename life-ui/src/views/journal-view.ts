import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface CaseTypeStats {
  caseType: string;
  total: number;
  active: number;
  completed: number;
  failed: number;
  completionRate: number | null;
}

interface DomainSlaStats {
  domain: string;
  totalWithSla: number;
  breachedCount: number;
  complianceRate: number | null;
}

const DOMAIN_LABELS: Record<string, string> = {
  HOUSEHOLD: 'Household', HEALTH: 'Health', FINANCE: 'Finance',
  FAMILY_SCHEDULING: 'Family', TRAVEL: 'Travel', LEGAL: 'Legal',
  CONTRACTOR_COORDINATION: 'Contractor', ELDER_CARE: 'Elder Care',
};

const CASE_LABELS: Record<string, string> = {
  'contractor-coordination': 'Contractor', 'care-coordination': 'Care',
  'travel-plan': 'Travel', 'appointment-cycle': 'Appointment',
  'financial-review': 'Finance', 'home-maintenance': 'Home',
};

@customElement('journal-view')
export class JournalView extends LitElement {
  static override styles = css`
    :host {
      display: block; height: 100%; overflow-y: auto;
      padding: var(--pages-space-5, 20px);
      box-sizing: border-box;
    }
    h2 {
      font-size: var(--pages-font-size-xl, 20px); font-weight: 600;
      color: var(--pages-neutral-12, #111);
      margin: 0 0 var(--pages-space-5, 20px) 0;
    }
    .sections {
      display: flex; flex-direction: column;
      gap: var(--pages-space-5, 20px);
      max-width: 900px;
    }
    .card {
      background: var(--pages-neutral-1, #fafafa);
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-lg, 8px);
      padding: var(--pages-space-5, 20px);
    }
    .card h3 {
      margin: 0 0 var(--pages-space-4, 16px) 0;
      font-size: var(--pages-font-size-base, 15px);
      font-weight: 600; color: var(--pages-neutral-12, #111);
    }
    table {
      width: 100%; border-collapse: collapse;
      font-size: var(--pages-font-size-sm, 14px);
    }
    th {
      text-align: left;
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      font-weight: 500; color: var(--pages-neutral-8, #737373);
      border-bottom: 1px solid var(--pages-neutral-4, #d4d4d4);
      font-size: var(--pages-font-size-xs, 12px);
      text-transform: uppercase; letter-spacing: 0.05em;
    }
    td {
      padding: var(--pages-space-2, 8px) var(--pages-space-3, 12px);
      color: var(--pages-neutral-11, #262626);
      border-bottom: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
    td.num { text-align: right; font-variant-numeric: tabular-nums; }
    .compliance-bar {
      display: flex; align-items: center; gap: var(--pages-space-2, 8px);
    }
    .bar-track {
      flex: 1; height: 6px; border-radius: 3px;
      background: var(--pages-neutral-3, #f0f0f0); overflow: hidden;
    }
    .bar-fill { height: 100%; border-radius: 3px; }
    .bar-fill[data-good] { background: var(--pages-green-9, #16a34a); }
    .bar-fill[data-warn] { background: var(--pages-amber-9, #d97706); }
    .bar-fill[data-bad] { background: var(--pages-red-9, #dc2626); }
    .bar-label { font-size: var(--pages-font-size-xs, 12px); min-width: 36px; text-align: right; }
    .stat-grid {
      display: grid; grid-template-columns: repeat(3, 1fr);
      gap: var(--pages-space-4, 16px);
    }
    .stat {
      text-align: center;
      padding: var(--pages-space-3, 12px);
      border: 1px solid var(--pages-neutral-3, #f0f0f0);
      border-radius: var(--pages-radius-md, 6px);
    }
    .stat-value {
      font-size: var(--pages-font-size-xl, 20px); font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }
    .stat-label {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      margin-top: var(--pages-space-1, 4px);
    }
    .loading { color: var(--pages-neutral-8, #737373); font-size: var(--pages-font-size-sm, 14px); }
  `;

  @state() private _caseStats: CaseTypeStats[] = [];
  @state() private _slaStats: DomainSlaStats[] = [];
  @state() private _trustAvg: number | null = null;
  @state() private _actorCount = 0;
  @state() private _loading = true;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetch();
  }

  private async _fetch(): Promise<void> {
    try {
      const [cases, sla, trust] = await Promise.all([
        fetch('/analytics/cases').then(r => r.ok ? r.json() : null),
        fetch('/analytics/sla').then(r => r.ok ? r.json() : null),
        fetch('/analytics/trust').then(r => r.ok ? r.json() : null),
      ]);
      this._caseStats = cases?.entries ?? [];
      this._slaStats = sla?.entries ?? [];
      this._trustAvg = trust?.avgGlobalScore ?? null;
      this._actorCount = trust?.actorCount ?? 0;
    } catch (e) { console.error(e); }
    finally { this._loading = false; }
  }

  override render() {
    if (this._loading) return html`<p class="loading">Loading journal…</p>`;

    const totalCases = this._caseStats.reduce((s, e) => s + e.total, 0);
    const activeCases = this._caseStats.reduce((s, e) => s + e.active, 0);
    const completedCases = this._caseStats.reduce((s, e) => s + e.completed, 0);

    return html`
      <h2>Journal</h2>
      <div class="sections">
        <div class="card">
          <h3>Overview</h3>
          <div class="stat-grid">
            <div class="stat"><div class="stat-value">${totalCases}</div><div class="stat-label">Total Cases</div></div>
            <div class="stat"><div class="stat-value">${activeCases}</div><div class="stat-label">Active</div></div>
            <div class="stat"><div class="stat-value">${completedCases}</div><div class="stat-label">Completed</div></div>
            <div class="stat"><div class="stat-value">${this._actorCount}</div><div class="stat-label">External Actors</div></div>
            <div class="stat"><div class="stat-value">${this._trustAvg != null ? (this._trustAvg * 100).toFixed(0) + '%' : '—'}</div><div class="stat-label">Trust Average</div></div>
            <div class="stat"><div class="stat-value">${this._slaStats.reduce((s, e) => s + e.breachedCount, 0)}</div><div class="stat-label">SLA Breaches</div></div>
          </div>
        </div>

        <div class="card">
          <h3>Cases by Type</h3>
          <table>
            <thead><tr><th>Type</th><th>Total</th><th>Active</th><th>Done</th><th>Failed</th><th>Rate</th></tr></thead>
            <tbody>
              ${this._caseStats.map(e => html`
                <tr>
                  <td>${CASE_LABELS[e.caseType] ?? e.caseType}</td>
                  <td class="num">${e.total}</td>
                  <td class="num">${e.active}</td>
                  <td class="num">${e.completed}</td>
                  <td class="num">${e.failed}</td>
                  <td class="num">${e.completionRate != null ? (e.completionRate * 100).toFixed(0) + '%' : '—'}</td>
                </tr>
              `)}
            </tbody>
          </table>
        </div>

        <div class="card">
          <h3>SLA Compliance by Domain</h3>
          ${this._slaStats.length > 0 ? html`
            <table>
              <thead><tr><th>Domain</th><th>Total</th><th>Breached</th><th>Compliance</th></tr></thead>
              <tbody>
                ${this._slaStats.map(e => {
                  const rate = e.complianceRate ?? 1;
                  const pct = (rate * 100).toFixed(0);
                  return html`
                    <tr>
                      <td>${DOMAIN_LABELS[e.domain] ?? e.domain}</td>
                      <td class="num">${e.totalWithSla}</td>
                      <td class="num">${e.breachedCount}</td>
                      <td>
                        <div class="compliance-bar">
                          <div class="bar-track">
                            <div class="bar-fill" style="width: ${pct}%"
                                 ?data-good=${rate >= 0.9} ?data-warn=${rate >= 0.7 && rate < 0.9} ?data-bad=${rate < 0.7}></div>
                          </div>
                          <span class="bar-label">${pct}%</span>
                        </div>
                      </td>
                    </tr>
                  `;
                })}
              </tbody>
            </table>
          ` : html`<p class="loading">No SLA data available</p>`}
        </div>
      </div>
    `;
  }
}
