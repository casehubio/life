import { LitElement, html, css, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';
import { LifeEventController, CALENDAR_EVENT_TYPES } from '../events/life-event-controller.js';

interface CalendarEvent {
  eventId: string;
  calendarId: string;
  summary: string;
  description: string | null;
  location: string | null;
  timing: { '@type': string; start: string; end: string; timeZone?: string };
  attendees: string[];
  domain: string | null;
  entityType: string | null;
  entityId: string | null;
  casehubManaged: boolean;
}

interface CalendarConflict {
  id: string;
  mappingId: string;
  conflictType: 'MODIFIED' | 'DELETED';
  detectedAt: string;
  externalSummary: string | null;
  externalTiming: { '@type': string; start: string; end: string; timeZone?: string } | null;
  status: 'DETECTED' | 'ACCEPTED' | 'DISMISSED';
  domain: string | null;
  entityId: string | null;
  casehubSummary: string | null;
  casehubTiming: { '@type': string; start: string; end: string; timeZone?: string } | null;
}

interface DayGroup {
  label: string;
  date: string;
  events: CalendarEvent[];
}

@customElement('life-calendar-panel')
export class CalendarPanel extends LitElement {
  static override styles = css`
    :host {
      display: block;
      padding: var(--pages-space-3, 12px);
      overflow-y: auto;
      height: 100%;
      box-sizing: border-box;
    }

    .header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: var(--pages-space-3, 12px);
    }

    h3 {
      margin: 0;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
    }

    .conflict-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 18px;
      height: 18px;
      padding: 0 4px;
      border-radius: 9px;
      background: var(--pages-amber-9, #d97706);
      color: white;
      font-size: 10px;
      font-weight: 700;
    }

    .refresh-btn {
      background: none;
      border: 1px solid var(--pages-neutral-5, #d4d4d4);
      border-radius: 4px;
      padding: 2px 6px;
      cursor: pointer;
      font-size: 11px;
      color: var(--pages-neutral-10, #404040);
    }
    .refresh-btn:hover { background: var(--pages-neutral-2, #f5f5f5); }
    .refresh-btn:disabled { opacity: 0.5; cursor: not-allowed; }

    .day {
      margin-bottom: var(--pages-space-3, 12px);
    }

    .day-label {
      font-size: var(--pages-font-size-xs, 12px);
      font-weight: 600;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--pages-neutral-8, #737373);
      margin-bottom: var(--pages-space-1, 4px);
    }

    .event {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      padding: var(--pages-space-1, 4px) var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-11, #262626);
      border-left: 3px solid var(--pages-neutral-5, #a3a3a3);
      margin-bottom: var(--pages-space-1, 4px);
      cursor: default;
    }

    .event[data-domain="HEALTH"] { border-color: var(--pages-green-9, #16a34a); }
    .event[data-domain="CONTRACTOR_COORDINATION"] { border-color: var(--pages-amber-9, #d97706); }
    .event[data-domain="FAMILY_SCHEDULING"] { border-color: var(--pages-blue-9, #2563eb); }
    .event[data-domain="LEGAL"] { border-color: var(--pages-red-9, #dc2626); }
    .event[data-domain="ELDER_CARE"] { border-color: var(--pages-purple-9, #7c3aed); }
    .event[data-domain="HOUSEHOLD"] { border-color: var(--pages-neutral-7, #a3a3a3); }
    .event[data-domain="FINANCE"] { border-color: var(--pages-teal-9, #0d9488); }
    .event[data-domain="TRAVEL"] { border-color: var(--pages-sky-9, #0284c7); }

    .event.conflicted { border-left-style: dashed; }

    .time {
      color: var(--pages-neutral-8, #737373);
      min-width: 40px;
    }

    .managed-icon {
      font-size: 10px;
      color: var(--pages-blue-8, #3b82f6);
    }

    .conflict-icon {
      color: var(--pages-amber-9, #d97706);
      font-size: 12px;
    }

    .empty {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-7, #a3a3a3);
      font-style: italic;
      text-align: center;
      padding: var(--pages-space-3, 12px);
    }

    .loading {
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-7, #a3a3a3);
      text-align: center;
      padding: var(--pages-space-3, 12px);
    }

    .conflicts-section {
      margin-bottom: var(--pages-space-3, 12px);
    }

    .conflict-card {
      border: 1px solid var(--pages-amber-5, #fcd34d);
      border-radius: 6px;
      padding: var(--pages-space-2, 8px);
      margin-bottom: var(--pages-space-2, 8px);
      background: var(--pages-amber-1, #fffbeb);
      font-size: var(--pages-font-size-xs, 12px);
    }

    .conflict-card.deleted {
      border-style: dashed;
      background: var(--pages-red-1, #fef2f2);
      border-color: var(--pages-red-5, #fca5a5);
    }

    .conflict-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: var(--pages-space-1, 4px);
    }

    .conflict-type {
      font-weight: 600;
      color: var(--pages-amber-11, #92400e);
    }

    .conflict-card.deleted .conflict-type {
      color: var(--pages-red-11, #991b1b);
    }

    .diff-row {
      display: grid;
      grid-template-columns: 60px 1fr 20px 1fr;
      gap: 4px;
      align-items: center;
      margin-bottom: 2px;
    }

    .diff-label {
      font-weight: 500;
      color: var(--pages-neutral-8, #737373);
    }

    .diff-old {
      text-decoration: line-through;
      color: var(--pages-neutral-8, #737373);
    }

    .diff-arrow { text-align: center; color: var(--pages-neutral-6, #a3a3a3); }

    .conflict-actions {
      display: flex;
      gap: var(--pages-space-2, 8px);
      margin-top: var(--pages-space-2, 8px);
    }

    .btn-accept {
      padding: 2px 8px;
      border: none;
      border-radius: 4px;
      background: var(--pages-blue-9, #2563eb);
      color: white;
      font-size: 11px;
      cursor: pointer;
    }
    .btn-accept:hover { background: var(--pages-blue-10, #1d4ed8); }

    .btn-dismiss {
      padding: 2px 8px;
      border: 1px solid var(--pages-neutral-5, #d4d4d4);
      border-radius: 4px;
      background: transparent;
      font-size: 11px;
      cursor: pointer;
      color: var(--pages-neutral-10, #404040);
    }
    .btn-dismiss:hover { background: var(--pages-neutral-2, #f5f5f5); }
  `;

  @state() private _days: DayGroup[] = [];
  @state() private _conflicts: CalendarConflict[] = [];
  @state() private _conflictCount = 0;
  @state() private _loading = true;
  @state() private _syncing = false;
  @state() private _error = false;

  private _sseController = new LifeEventController(this, {
    types: CALENDAR_EVENT_TYPES,
    onEvent: () => this._refresh(),
  });

  override connectedCallback() {
    super.connectedCallback();
    this._refresh();
  }

  private async _refresh() {
    await Promise.all([this._fetchEvents(), this._fetchConflicts()]);
  }

  private async _fetchEvents() {
    this._loading = true;
    try {
      const now = new Date();
      const weekEnd = new Date(now.getTime() + 7 * 86400 * 1000);
      const from = now.toISOString();
      const to = weekEnd.toISOString();
      const res = await fetch(`/life-calendar/events?from=${from}&to=${to}`);
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const events: CalendarEvent[] = await res.json();
      this._days = this._groupByDay(events);
      this._error = false;
    } catch {
      this._error = true;
      this._days = [];
    } finally {
      this._loading = false;
    }
  }

  private async _fetchConflicts() {
    try {
      const [conflictsRes, countRes] = await Promise.all([
        fetch('/life-calendar/conflicts'),
        fetch('/life-calendar/conflicts/count'),
      ]);
      if (conflictsRes.ok) this._conflicts = await conflictsRes.json();
      if (countRes.ok) this._conflictCount = parseInt(await countRes.text(), 10);
    } catch {
      this._conflicts = [];
      this._conflictCount = 0;
    }
  }

  private async _triggerSync() {
    this._syncing = true;
    try {
      await fetch('/life-calendar/sync', { method: 'POST' });
      await this._refresh();
    } finally {
      this._syncing = false;
    }
  }

  private async _resolveConflict(id: string, action: 'ACCEPT' | 'DISMISS') {
    await fetch(`/life-calendar/conflicts/${id}/resolve`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ action }),
    });
    await this._refresh();
  }

  private _groupByDay(events: CalendarEvent[]): DayGroup[] {
    const groups = new Map<string, CalendarEvent[]>();
    const dayNames = ['Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday'];
    for (const e of events) {
      const date = new Date(e.timing.start);
      const key = date.toISOString().slice(0, 10);
      if (!groups.has(key)) groups.set(key, []);
      groups.get(key)!.push(e);
    }
    return Array.from(groups.entries())
      .sort(([a], [b]) => a.localeCompare(b))
      .map(([date, evts]) => ({
        label: dayNames[new Date(date + 'T00:00:00').getDay()],
        date,
        events: evts.sort((a, b) => a.timing.start.localeCompare(b.timing.start)),
      }));
  }

  private _formatTime(timing: CalendarEvent['timing']): string {
    if (timing['@type'] === 'AllDay') return 'All day';
    const date = new Date(timing.start);
    return date.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' });
  }

  private _formatTimingShort(timing: { '@type': string; start: string } | null): string {
    if (!timing) return '—';
    if (timing['@type'] === 'AllDay') return timing.start;
    return new Date(timing.start).toLocaleDateString('en-GB', { day: 'numeric', month: 'short' });
  }

  private _eventHasConflict(eventId: string): boolean {
    return this._conflicts.some(c => c.entityId === eventId && c.status === 'DETECTED');
  }

  override render() {
    if (this._loading) return html`<div class="loading">Loading calendar...</div>`;
    if (this._error) return html`<div class="empty">Unable to load calendar</div>`;

    return html`
      <div class="header">
        <h3>This Week ${this._conflictCount > 0 ? html`<span class="conflict-badge">${this._conflictCount}</span>` : nothing}</h3>
        <div class="header-actions">
          <button class="refresh-btn" @click=${this._triggerSync} ?disabled=${this._syncing}>
            ${this._syncing ? 'Syncing...' : 'Sync'}
          </button>
        </div>
      </div>

      ${this._conflicts.length > 0 ? html`
        <div class="conflicts-section">
          ${this._conflicts.map(c => this._renderConflictCard(c))}
        </div>
      ` : nothing}

      ${this._days.length === 0 ? html`<div class="empty">No events this week</div>` : nothing}

      ${this._days.map(day => html`
        <div class="day">
          <div class="day-label">${day.label}</div>
          ${day.events.map(e => html`
            <div class="event ${this._eventHasConflict(e.entityId ?? '') ? 'conflicted' : ''}" data-domain="${e.domain ?? ''}">
              <span class="time">${this._formatTime(e.timing)}</span>
              <span>${e.summary}</span>
              ${e.casehubManaged ? html`<span class="managed-icon" title="Managed by CaseHub">&#9679;</span>` : nothing}
              ${this._eventHasConflict(e.entityId ?? '') ? html`<span class="conflict-icon" title="External edit detected">&#9888;</span>` : nothing}
            </div>
          `)}
        </div>
      `)}
    `;
  }

  private _renderConflictCard(c: CalendarConflict) {
    const isDeleted = c.conflictType === 'DELETED';
    return html`
      <div class="conflict-card ${isDeleted ? 'deleted' : ''}">
        <div class="conflict-header">
          <span class="conflict-type">${isDeleted ? 'Event Deleted' : 'Event Modified'}</span>
          <span style="font-size:10px;color:var(--pages-neutral-7)">${c.domain}</span>
        </div>

        ${!isDeleted ? html`
          ${c.casehubSummary !== c.externalSummary && c.externalSummary ? html`
            <div class="diff-row">
              <span class="diff-label">Title</span>
              <span class="diff-old">${c.casehubSummary ?? '—'}</span>
              <span class="diff-arrow">&rarr;</span>
              <span>${c.externalSummary}</span>
            </div>
          ` : nothing}
          ${c.externalTiming ? html`
            <div class="diff-row">
              <span class="diff-label">When</span>
              <span class="diff-old">${this._formatTimingShort(c.casehubTiming)}</span>
              <span class="diff-arrow">&rarr;</span>
              <span>${this._formatTimingShort(c.externalTiming)}</span>
            </div>
          ` : nothing}
        ` : html`
          <div style="color:var(--pages-red-10)">
            "${c.casehubSummary ?? 'Unknown event'}" was deleted from the external calendar.
          </div>
        `}

        <div class="conflict-actions">
          <button class="btn-accept" @click=${() => this._resolveConflict(c.id, 'ACCEPT')}>
            ${isDeleted ? 'Remove Mapping' : 'Accept Change'}
          </button>
          <button class="btn-dismiss" @click=${() => this._resolveConflict(c.id, 'DISMISS')}>
            ${isDeleted ? 'Recreate Event' : 'Restore CaseHub'}
          </button>
        </div>
      </div>
    `;
  }
}
