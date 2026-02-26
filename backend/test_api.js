const axios = require('axios');

async function testApi() {
    try {
        console.log('Testing Prices Endpoint...');
        const prices = await axios.get('http://localhost:3000/v1/prices?symbols=BTC,ETH');
        console.log('Prices Response:', JSON.stringify(prices.data, null, 2));

        console.log('\nTesting History Endpoint...');
        const history = await axios.get('http://localhost:3000/v1/history?symbol=ETH&limit=5');
        console.log('History Response (length):', history.data.length);
        console.log('Sample History Item:', JSON.stringify(history.data[0], null, 2));
    } catch (e) {
        console.error('API Test Failed:', e.message);
    }
}

testApi();
