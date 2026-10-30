import { registerPanel } from '@casehubio/pages-runtime';

import './centre-content.js';
import './morning-briefing.js';
import './action-items.js';
import './inbox-dock.js';
import './cases-dock.js';
import './family-summary.js';
import './calendar-panel.js';
import './money-mock.js';
import './comms-mock.js';

registerPanel('life-centre-content', 'life-centre-content');
registerPanel('life-inbox-dock', 'life-inbox-dock');
registerPanel('life-cases-dock', 'life-cases-dock');
registerPanel('life-calendar-panel', 'life-calendar-panel');
registerPanel('life-family-summary', 'life-family-summary');
registerPanel('life-money-mock', 'life-money-mock');
registerPanel('life-comms-mock', 'life-comms-mock');
