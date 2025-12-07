const https = require('https');
const WebSocket = require('ws');

const BASE_URL = 'livetiming.formula1.com';
const USER_AGENT = 'Mozilla/5.0';

console.log('🔌 Connecting to check tyre data in TimingAppData...');

const negotiateUrl = `/signalr/negotiate?clientProtocol=1.5&connectionData=${encodeURIComponent('[{"name":"Streaming"}]')}`;

https.get({ hostname: BASE_URL, path: negotiateUrl, headers: { 'User-Agent': USER_AGENT } }, (res) => {
    let data = '';
    res.on('data', chunk => data += chunk);
    res.on('end', () => {
        const negotiate = JSON.parse(data);

        const encodedToken = encodeURIComponent(negotiate.ConnectionToken);
        const encodedData = encodeURIComponent('[{"name":"Streaming"}]');
        const wsUrl = `wss://${BASE_URL}/signalr/connect?transport=webSockets&clientProtocol=1.5&connectionToken=${encodedToken}&connectionData=${encodedData}`;

        const ws = new WebSocket(wsUrl, { headers: { 'User-Agent': USER_AGENT } });

        ws.on('open', () => {
            console.log('✅ Connected');
            ws.send(JSON.stringify({
                H: 'Streaming',
                M: 'Subscribe',
                A: [['TimingData', 'TimingAppData', 'DriverList']],
                I: 1
            }));
        });

        let msgCount = 0;

        ws.on('message', (rawData) => {
            try {
                const msg = JSON.parse(rawData.toString());

                // Check initial response R field
                if (msg.R && typeof msg.R === 'object') {
                    console.log('\n📦 INITIAL RESPONSE keys:', Object.keys(msg.R));

                    if (msg.R.TimingAppData && msg.R.TimingAppData.Lines) {
                        console.log('\n📦 TimingAppData.Lines sample:');
                        Object.entries(msg.R.TimingAppData.Lines).slice(0, 5).forEach(([num, data]) => {
                            console.log(`  Driver ${num}:`, JSON.stringify(data));
                        });
                    }

                    if (msg.R.TimingData && msg.R.TimingData.Lines) {
                        console.log('\n📦 TimingData.Lines sample (first 2):');
                        Object.entries(msg.R.TimingData.Lines).slice(0, 2).forEach(([num, data]) => {
                            console.log(`  Driver ${num}:`, JSON.stringify(data).substring(0, 500));
                        });
                    }
                }

                if (msg.M && msg.M.length > 0) {
                    msg.M.forEach(hubMsg => {
                        if (hubMsg.M === 'feed' && hubMsg.A) {
                            const topic = hubMsg.A[0];
                            const payload = hubMsg.A[1];

                            if (topic === 'TimingAppData' && payload?.Lines && msgCount < 5) {
                                Object.entries(payload.Lines).forEach(([num, data]) => {
                                    console.log(`🔧 TimingAppData Driver ${num}:`, JSON.stringify(data));
                                    msgCount++;
                                });
                            }
                        }
                    });
                }

                if (msgCount >= 5) {
                    ws.close();
                    process.exit(0);
                }
            } catch (e) { }
        });

        setTimeout(() => {
            ws.close();
            process.exit(0);
        }, 15000);
    });
});
