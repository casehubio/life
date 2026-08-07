import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import '@casehubio/blocks-ui-kpi-metric-row';
import { LifeEventController, ALL_EVENT_TYPES } from '../events/life-event-controller.js';
import './morning-briefing.js';
import './action-items.js';
import './active-cases.js';

@customElement('life-centre-content')
export class CentreContent extends LitElement {
  private _events = new LifeEventController(this, {
    types: ALL_EVENT_TYPES,
    onEvent: () => this._loadKpiData(),
  });

  static override styles = css`
    :host {
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-5, 20px);
      padding: var(--pages-space-5, 20px);
      overflow-y: auto;
      height: 100%;
      box-sizing: border-box;
    }

    section h3 {
      margin: 0 0 var(--pages-space-3, 12px) 0;
      font-size: var(--pages-font-size-base, 15px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }
  `;

  override render() {
    return html`
      <life-morning-briefing></life-morning-briefing>

      <blocks-kpi-metric-row
        density="compact"
        .metrics=${[
          { key: 'active-cases', label: 'Active Cases', value: '—', status: 'normal' as const },
          { key: 'sla-compliance', label: 'SLA Compliance', value: '—', unit: '%', status: 'normal' as const },
          { key: 'pending-actions', label: 'Pending', value: '—', status: 'normal' as const },
          { key: 'due-today', label: 'Due Today', value: '—', status: 'normal' as const },
          { key: 'trust-avg', label: 'Trust Avg', value: '—', status: 'normal' as const },
        ]}
      ></blocks-kpi-metric-row>

      <life-action-items></life-action-items>

      <life-active-cases></life-active-cases>
    `;
  }

  override async firstUpdated(): Promise<void> {
    await this._loadKpiData();
  }

  private async _loadKpiData(): Promise<void> {
    const kpi = this.renderRoot.querySelector('blocks-kpi-metric-row');
    if (!kpi) return;

    try {
      const [cases, sla, trust, actions] = await Promise.all([
        fetch('/analytics/cases').then(r => r.ok ? r.json() : null),
        fetch('/analytics/sla').then(r => r.ok ? r.json() : null),
        fetch('/analytics/trust').then(r => r.ok ? r.json() : null),
        fetch('/pending-actions?size=1').then(r => r.ok ? r.json() : null),
      ]);

      const activeCases = cases?.entries?.reduce((sum: number, e: { active: number }) => sum + e.active, 0) ?? 0;
      const overallCompliance = sla?.entries?.length > 0
        ? Math.round(sla.entries.reduce((sum: number, e: { complianceRate: number }) => sum + (e.complianceRate ?? 100), 0) / sla.entries.length)
        : 100;
      const pendingCount = actions?.totalCount ?? 0;
      const avgTrust = trust?.avgGlobalScore != null ? (trust.avgGlobalScore * 100).toFixed(0) : '—';

      const dueTodayRes = await fetch('/pending-actions?size=1&dueSoonHours=12');
      const dueToday = dueTodayRes.ok ? (await dueTodayRes.json()).totalCount : 0;

      (kpi as any).metrics = [
        { key: 'active-cases', label: 'Active Cases', value: activeCases, status: 'normal' },
        { key: 'sla-compliance', label: 'SLA Compliance', value: overallCompliance, unit: '%',
          status: overallCompliance < 90 ? 'warning' : 'normal' },
        { key: 'pending-actions', label: 'Pending', value: pendingCount,
          status: pendingCount > 5 ? 'warning' : 'normal' },
        { key: 'due-today', label: 'Due Today', value: dueToday,
          status: dueToday > 0 ? 'critical' : 'normal' },
        { key: 'trust-avg', label: 'Trust Avg', value: avgTrust, unit: '%',
          status: (trust?.avgGlobalScore ?? 1) < 0.6 ? 'warning' : 'normal' },
      ];
    } catch (e) { console.error(e);
      // KPI stays at placeholder values
    }
  }
}
