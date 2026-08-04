import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';

@customElement('life-calendar-mock')
export class CalendarMock extends LitElement {
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

    .event[data-domain="health"] { border-color: var(--pages-green-9, #16a34a); }
    .event[data-domain="contractor"] { border-color: var(--pages-amber-9, #d97706); }
    .event[data-domain="family"] { border-color: var(--pages-blue-9, #2563eb); }
    .event[data-domain="legal"] { border-color: var(--pages-red-9, #dc2626); }
    .event[data-domain="elder-care"] { border-color: var(--pages-purple-9, #7c3aed); }

    .time {
      color: var(--pages-neutral-8, #737373);
      min-width: 40px;
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
      <h3>This Week</h3>

      <div class="day">
        <div class="day-label">Monday</div>
        <div class="event" data-domain="health">
          <span class="time">09:30</span>
          <span>Jean — GP check-up</span>
        </div>
        <div class="event" data-domain="family">
          <span class="time">15:45</span>
          <span>Ella — swimming pick-up</span>
        </div>
      </div>

      <div class="day">
        <div class="day-label">Tuesday</div>
        <div class="event" data-domain="family">
          <span class="time">19:00</span>
          <span>Parent evening — Ella's school</span>
        </div>
      </div>

      <div class="day">
        <div class="day-label">Wednesday</div>
        <div class="event" data-domain="family">
          <span class="time">16:00</span>
          <span>Tom — football training</span>
        </div>
      </div>

      <div class="day">
        <div class="day-label">Thursday</div>
        <div class="event" data-domain="contractor">
          <span class="time">14:00</span>
          <span>Bob's Plumbing — boiler service</span>
        </div>
        <div class="event" data-domain="elder-care">
          <span class="time">17:00</span>
          <span>Jean — carer handover</span>
        </div>
      </div>

      <div class="day">
        <div class="day-label">Friday</div>
        <div class="event" data-domain="legal">
          <span class="time">10:00</span>
          <span>Harris & Co — contract review call</span>
        </div>
      </div>

      <p class="coming-soon">Coming soon — Calendar integration</p>
    `;
  }
}
