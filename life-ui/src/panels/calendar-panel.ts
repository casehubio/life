import { LitElement, html, css, nothing } from 'lit';
import { customElement, state } from 'lit/decorators.js';

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

    h3 {
      margin: 0 0 var(--pages-space-3, 12px) 0;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }

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
    }

    .event[data-domain="HEALTH"] { border-color: var(--pages-green-9, #16a34a); }
    .event[data-domain="CONTRACTOR_COORDINATION"] { border-color: var(--pages-amber-9, #d97706); }
    .event[data-domain="FAMILY_SCHEDULING"] { border-color: var(--pages-blue-9, #2563eb); }
    .event[data-domain="LEGAL"] { border-color: var(--pages-red-9, #dc2626); }
    .event[data-domain="ELDER_CARE"] { border-color: var(--pages-purple-9, #7c3aed); }
    .event[data-domain="HOUSEHOLD"] { border-color: var(--pages-neutral-7, #a3a3a3); }
    .event[data-domain="FINANCE"] { border-color: var(--pages-teal-9, #0d9488); }
    .event[data-domain="TRAVEL"] { border-color: var(--pages-sky-9, #0284c7); }

    .time {
      color: var(--pages-neutral-8, #737373);
      min-width: 40px;
    }

    .managed-icon {
      font-size: 10px;
      color: var(--pages-blue-8, #3b82f6);
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
  `;

  @state() private _days: DayGroup[] = [];
  @state() private _loading = true;
  @state() private _error = false;

  override connectedCallback() {
    super.connectedCallback();
    this._fetchEvents();
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

  override render() {
    if (this._loading) return html`<div class="loading">Loading calendar...</div>`;
    if (this._error) return html`<div class="empty">Unable to load calendar</div>`;
    if (this._days.length === 0) return html`<h3>This Week</h3><div class="empty">No events this week</div>`;

    return html`
      <h3>This Week</h3>
      ${this._days.map(day => html`
        <div class="day">
          <div class="day-label">${day.label}</div>
          ${day.events.map(e => html`
            <div class="event" data-domain="${e.domain ?? ''}">
              <span class="time">${this._formatTime(e.timing)}</span>
              <span>${e.summary}</span>
              ${e.casehubManaged ? html`<span class="managed-icon" title="Managed by CaseHub">&#9679;</span>` : nothing}
            </div>
          `)}
        </div>
      `)}
    `;
  }
}
