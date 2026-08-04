import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import '@casehubio/blocks-ui-grouped-data-view';

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

    h3 {
      margin: 0 0 var(--pages-space-3, 12px) 0;
      font-size: var(--pages-font-size-sm, 14px);
      font-weight: 600;
      color: var(--pages-neutral-12, #111);
    }
  `;

  override render() {
    return html`
      <h3>Active Cases</h3>
      <blocks-grouped-data-view
        endpoint="/life-cases?status=ACTIVE"
        groupBy="caseType"
        preset="list"
        .defaultExpanded=${true}
      ></blocks-grouped-data-view>
    `;
  }
}
