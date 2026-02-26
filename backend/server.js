const express = require('express');
const axios = require('axios');
const cors = require('cors');

const app = express();
const PORT = process.env.PORT || 3000;

app.use(cors());
app.use(express.json());

// In-memory cache
let priceCache = {
    data: null,
    timestamp: 0
};

const CACHE_DURATION = 60 * 1000; // 60 seconds

/**
 * Fetch prices from Binance (Centralized exchange, very fast)
 */
async function fetchBinancePrices() {
    try {
        const response = await axios.get('https://api.binance.com/api/v3/ticker/price');
        // Convert to a map for easy lookup: { "ETHUSDT": 2500.0, ... }
        return response.data.reduce((acc, item) => {
            acc[item.symbol] = parseFloat(item.price);
            return acc;
        }, {});
    } catch (e) {
        console.error('Binance fetch failed:', e.message);
        return null;
    }
}

/**
 * Fetch prices from CryptoCompare (Great for broad market coverage)
 */
async function fetchCryptoComparePrices(symbols) {
    try {
        const response = await axios.get(`https://min-api.cryptocompare.com/data/pricemulti?fsyms=${symbols.join(',')}&tsyms=USD`);
        return response.data;
    } catch (e) {
        console.error('CryptoCompare fetch failed:', e.message);
        return null;
    }
}

app.get('/v1/prices', async (req, res) => {
    const symbols = req.query.symbols ? req.query.symbols.split(',') : ['BTC', 'ETH', 'BNB', 'MATIC', 'TRX', 'ARB', 'OP', 'BASE'];

    // Check cache
    const now = Date.now();
    if (priceCache.data && (now - priceCache.timestamp < CACHE_DURATION)) {
        return res.json({ source: 'cache', prices: priceCache.data });
    }

    try {
        // Parallel fetch for speed
        const [binanceData, cryptoCompareData] = await Promise.all([
            fetchBinancePrices(),
            fetchCryptoComparePrices(symbols)
        ]);

        const aggregatedPrices = {};

        symbols.forEach(symbol => {
            const binanceSymbol = symbol === 'BTC' ? 'BTCUSDT' :
                symbol === 'ETH' ? 'ETHUSDT' :
                    symbol === 'BNB' ? 'BNBUSDT' :
                        symbol === 'MATIC' || symbol === 'POL' ? 'MATICUSDT' :
                            `${symbol}USDT`;

            const bPrice = binanceData ? binanceData[binanceSymbol] : null;
            const cPrice = cryptoCompareData && cryptoCompareData[symbol] ? cryptoCompareData[symbol].USD : null;

            // Simple aggregation: Prefer Binance for liquidity, fallback to CryptoCompare
            aggregatedPrices[symbol] = bPrice || cPrice || 0.0;
        });

        // Update cache
        priceCache = {
            data: aggregatedPrices,
            timestamp: now
        };

        res.json({ source: 'live', prices: aggregatedPrices });
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch prices' });
    }
});

app.get('/v1/history', async (req, res) => {
    const { symbol, limit = 24 } = req.query;
    if (!symbol) return res.status(400).json({ error: 'Symbol is required' });

    try {
        const response = await axios.get(`https://min-api.cryptocompare.com/data/v2/histohour?fsym=${symbol}&tsym=USD&limit=${limit}`);
        res.json(response.data.Data.Data);
    } catch (error) {
        res.status(500).json({ error: 'Failed to fetch history' });
    }
});

app.listen(PORT, () => {
    console.log(`Price Aggregator Backend running on port ${PORT}`);
});
