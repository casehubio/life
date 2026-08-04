import { LitElement, html, css } from 'lit';
import { customElement } from 'lit/decorators.js';
import type { WorkIdentity } from '@casehubio/blocks-ui-core';
import '@casehubio/blocks-ui-work-item-inbox';

const DEMO_IDENTITY: WorkIdentity = {
  userId: 'demo-admin',
  displayName: 'Mark',
  groups: ['household-admin'],
  tenancyId: '278776f9-e1b0-46fb-9032-8bddebdcf9ce',
};

@customElement('life-inbox-dock')
export class InboxDock extends LitElement {
  static override styles = css`
    :host {
      display: block;
      height: 100%;
      overflow-y: auto;
    }

    blocks-work-item-inbox {
      height: 100%;
      --wi-density: compact;
    }
  `;

  override render() {
    return html`
      <blocks-work-item-inbox
        .identity=${DEMO_IDENTITY}
        endpoint="/pending-actions"
        mode="compact"
      ></blocks-work-item-inbox>
    `;
  }
}
