package com.github.jnidzwetzki.bitfinex.v2.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.github.jnidzwetzki.bitfinex.v2.BitfinexApiBroker;
import com.github.jnidzwetzki.bitfinex.v2.callback.channel.TickHandler;
import com.github.jnidzwetzki.bitfinex.v2.entity.APIException;
import com.github.jnidzwetzki.bitfinex.v2.entity.symbol.BitfinexCurrencyPair;
import com.github.jnidzwetzki.bitfinex.v2.manager.QuoteManager;


public class TickHandlerTest {

	/**
	 * The delta for double compares
	 */
	private static final double DELTA = 0.001;

	/**
	 * Test the parsing of one tick
	 * @throws APIException
	 * @throws InterruptedException 
	 */
	@Test(timeout=20000)
	public void testTickUpdateAndNotify() throws APIException, InterruptedException {
		
		final String callbackValue = "[26123,41.4645776,26129,33.68138507,2931,0.2231,26129,144327.10936387,26149,13139]";
		final JSONArray jsonArray = new JSONArray(callbackValue);
		final BitfinexCurrencyPair currencyPair = BitfinexCurrencyPair.BTC_USD;
		
		final ExecutorService executorService = Executors.newFixedThreadPool(10);
		final BitfinexApiBroker bitfinexApiBroker = Mockito.mock(BitfinexApiBroker.class);
		Mockito.when(bitfinexApiBroker.getExecutorService()).thenReturn(executorService);
		final QuoteManager tickerManager = new QuoteManager(bitfinexApiBroker);
		Mockito.when(bitfinexApiBroker.getQuoteManager()).thenReturn(tickerManager);

		final CountDownLatch latch = new CountDownLatch(1);

		tickerManager.registerTickCallback(currencyPair, (s, c) -> {
			
			try {
				Assert.assertEquals(currencyPair, s);
				Assert.assertEquals(26129, c.getOpenPrice().toDouble(), DELTA);
				Assert.assertEquals(26129, c.getClosePrice().toDouble(), DELTA);
				Assert.assertEquals(26129, c.getMaxPrice().toDouble(), DELTA);
				Assert.assertEquals(26129, c.getMinPrice().toDouble(), DELTA);
				Assert.assertEquals(0, c.getVolume().toDouble(), DELTA);
			} catch(Throwable e) {
				System.out.println(e);
				throw e;
			}
			
			latch.countDown();
		});
		
		
		Assert.assertEquals(-1, tickerManager.getHeartbeatForSymbol(currencyPair));
		Assert.assertEquals(null, tickerManager.getLastTick(currencyPair));

		final TickHandler tickHandler = new TickHandler();
		final long now = System.currentTimeMillis();
		tickHandler.handleChannelData(bitfinexApiBroker, currencyPair, jsonArray);
		
		// Tick callbacks are handled async
		latch.await();
		Assert.assertTrue(now <= tickerManager.getHeartbeatForSymbol(currencyPair));
		Assert.assertTrue(tickerManager.getLastTick(currencyPair) != null);
		Assert.assertTrue(tickerManager.getLastTick(currencyPair) != null);
	}
	
}
