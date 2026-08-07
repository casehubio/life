import { LitElement, html, css } from 'lit';
import { customElement, state } from 'lit/decorators.js';

interface FamilyMember {
  name: string;
  role: string;
  age?: number;
  pendingTasks: number;
  activeCases: number;
  mockLines: string[];
}

const HOUSEHOLD: FamilyMember[] = [
  {
    name: 'Mark', role: 'Admin', pendingTasks: 0, activeCases: 0,
    mockLines: ['3 approvals pending', 'Contractor oversight active'],
  },
  {
    name: 'Sarah', role: 'Member', pendingTasks: 0, activeCases: 0,
    mockLines: ['GP follow-up due', 'School trip consent pending'],
  },
  {
    name: 'Ella', role: 'Junior', age: 15, pendingTasks: 0, activeCases: 0,
    mockLines: ['Swimming cancelled this week', 'Dentist overdue'],
  },
  {
    name: 'Tom', role: 'Junior', age: 11, pendingTasks: 0, activeCases: 0,
    mockLines: ['Football Sat 10am', 'School shoes needed'],
  },
  {
    name: 'Grandma Jean', role: 'External', pendingTasks: 0, activeCases: 0,
    mockLines: ['Carer visit Thu 5pm', 'Medication review due'],
  },
];

@customElement('life-family-summary')
export class FamilySummary extends LitElement {
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

    .members {
      display: flex;
      flex-direction: column;
      gap: var(--pages-space-3, 12px);
    }

    .member {
      border: 1px solid var(--pages-neutral-4, #d4d4d4);
      border-radius: var(--pages-radius-md, 6px);
      padding: var(--pages-space-3, 12px);
      background: var(--pages-neutral-1, #fafafa);
    }

    .member-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: var(--pages-space-2, 8px);
    }

    .name {
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }

    .badge {
      font-size: var(--pages-font-size-xs, 12px);
      padding: 1px 6px;
      border-radius: var(--pages-radius-sm, 4px);
      background: var(--pages-neutral-3, #f0f0f0);
      color: var(--pages-neutral-9, #525252);
    }

    .badge[data-role="Admin"] {
      background: var(--pages-accent-3, #e0e7ff);
      color: var(--pages-accent-11, #3730a3);
    }

    .line {
      display: flex;
      align-items: center;
      gap: var(--pages-space-2, 8px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-9, #525252);
      padding: var(--pages-space-1, 4px) 0;
    }

    .line .tag {
      font-size: 10px;
      padding: 0 4px;
      border-radius: 3px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .tag-real {
      background: var(--pages-green-3, #dcfce7);
      color: var(--pages-green-11, #166534);
    }

    .tag-mock {
      background: var(--pages-neutral-3, #f0f0f0);
      color: var(--pages-neutral-8, #737373);
    }

    .stats {
      display: flex;
      gap: var(--pages-space-3, 12px);
      font-size: var(--pages-font-size-xs, 12px);
      color: var(--pages-neutral-8, #737373);
      margin-top: var(--pages-space-2, 8px);
      padding-top: var(--pages-space-2, 8px);
      border-top: 1px solid var(--pages-neutral-3, #f0f0f0);
    }
  `;

  @state() private _members = HOUSEHOLD;

  override connectedCallback(): void {
    super.connectedCallback();
    this._fetchRealData();
  }

  private async _fetchRealData(): Promise<void> {
    try {
      const res = await fetch('/pending-actions?size=100');
      if (!res.ok) return;
      const data = await res.json();
      const items: Array<{ candidateGroups: string }> = data.items ?? [];

      this._members = this._members.map(m => {
        const group = m.role === 'Admin' ? 'household-admin'
          : m.role === 'Junior' ? 'household-junior' : 'household-member';
        return {
          ...m,
          pendingTasks: items.filter(i => i.candidateGroups === group).length,
        };
      });
    } catch (e) { console.error(e);
      // Keep mock defaults
    }
  }

  override render() {
    return html`
      <h3>Family</h3>
      <div class="members">
        ${this._members.map(m => html`
          <div class="member">
            <div class="member-header">
              <span class="name">${m.name}${m.age ? ` (${m.age})` : ''}</span>
              <span class="badge" data-role=${m.role}>${m.role}</span>
            </div>
            ${m.mockLines.map(line => html`
              <div class="line">
                <span class="tag tag-mock">mock</span>
                <span>${line}</span>
              </div>
            `)}
            <div class="stats">
              <span>Tasks: ${m.pendingTasks}</span>
              <span>Cases: ${m.activeCases}</span>
            </div>
          </div>
        `)}
      </div>
    `;
  }
}
