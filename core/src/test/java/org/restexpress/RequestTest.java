/*
    Copyright 2011, Strategic Gains, Inc.

	Licensed under the Apache License, Version 2.0 (the "License");
	you may not use this file except in compliance with the License.
	You may obtain a copy of the License at

		http://www.apache.org/licenses/LICENSE-2.0

	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	See the License for the specific language governing permissions and
	limitations under the License.
*/
package org.restexpress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;

import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.restexpress.exception.BadRequestException;

/**
 * @author toddf
 * @since Mar 29, 2011
 */
public class RequestTest
{
	private Request request;

	@Before
	public void initialize()
	{
		FullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/foo?param1=bar&param2=blah&yada");
		httpRequest.headers().add("Host", "testing-host");
		request = new Request(httpRequest, null, null);
	}

	@Test
	public void shouldRetrieveEntireUrl()
	{
		assertEquals("https://testing-host/foo?param1=bar&param2=blah&yada", request.getUrl());
	}

	@Test
	public void shouldRetrieveBaseUrl()
	{
		assertEquals("https://testing-host", request.getBaseUrl());
	}

	@Test
	public void shouldRetrievePath()
	{
		assertEquals("/foo?param1=bar&param2=blah&yada", request.getPath());
	}

	@Test
	public void shouldApplyQueryStringParamsAsHeaders()
	{
		assertEquals("bar", request.getHeader("param1"));
		assertEquals("blah", request.getHeader("param2"));
		assertEquals("", request.getHeader("yada"));
	}
	
	@Test
	public void shouldParseQueryStringIntoMap()
	{
		Map<String, String> m = request.getQueryStringMap();
		assertNotNull(m);
		assertEquals("bar", m.get("param1"));
		assertEquals("blah", m.get("param2"));
		assertEquals("", m.get("yada"));
	}

	@Test
	public void shouldHandleNoQueryString()
	{
		Request r = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/foo"), null);
		Map<String, String> m = r.getQueryStringMap();
		assertNotNull(m);
        assertTrue(m.isEmpty());
	}

	@Test
	public void shouldHandleNullQueryString()
	{
		Request r = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/foo?"), null);
		Map<String, String> m = r.getQueryStringMap();
        assertNotNull(m);
        assertTrue(m.isEmpty());
	}

	@Test
	public void shouldHandleGoofyQueryString()
	{
		Request r = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/foo??&"), null);
		Map<String, String> m = r.getQueryStringMap();
		assertNotNull(m);
		assertEquals("", m.get("?"));
	}

	@Test
	public void shouldDecodeQueryStringMapKey()
	{
		Request r = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/foo?bar%26bat=true"), null);
		Map<String, String> m = r.getQueryStringMap();
		assertNotNull(m);
		assertEquals("true", m.get("bar&bat"));
		assertEquals("true", r.getHeader("bar&bat"));
	}

	@Test
	public void shouldDecodeUrlEncodedQueryStringMapValue()
	{
		Request r = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/foo?assertion=assertion%7CfitnesseIdm40%40ecollege.com%7C2013-03-14T18%3A02%3A08%2B00%3A00%7C2f6f1b0fa8ecce7d092c1c45cc44e4c7"), null);
		Map<String, String> m = r.getQueryStringMap();
		assertNotNull(m);
		assertEquals("assertion|fitnesseIdm40@ecollege.com|2013-03-14T18:02:08+00:00|2f6f1b0fa8ecce7d092c1c45cc44e4c7", m.get("assertion"));
		assertEquals("assertion|fitnesseIdm40@ecollege.com|2013-03-14T18:02:08+00:00|2f6f1b0fa8ecce7d092c1c45cc44e4c7", r.getHeader("assertion"));
	}

	@Test
	public void shouldSetHeaderOnInvalidUrlDecodedQueryString()
	{
		String key = "invalidUrlDecode";
		String value = "%invalid";
		Request r = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/foo?" + key + "=" + value), null);
		assertEquals(value, r.getHeader(key));
	}

	@Test
	public void shouldSetAndGetHeader()
	{
		String key = "header-key";
		String value = "header value";
		request.addHeader(key, value);
		assertEquals(value, request.getHeader(key));
	}

	@Test
	public void shouldSetAndGetInvalidUrlEncodedHeader()
	{
		String key = "invalid-header-key";
		String value = "%invalidUrlEncode";
		request.addHeader(key, value);
		assertEquals(value, request.getHeader(key));
	}

	@Test
	public void shouldNotAlterUrlEncodedHeader()
	{
		String key = "validUrlDecode";
		String value = "%20this%20that";
		request.addHeader(key, value);
		assertEquals(value, request.getHeader(key));
	}

	@Test
	public void shouldNotAlterUrlDecodedHeaderWithMessage()
	{
		String key = "validUrlDecode";
		String value = "%20this%20that";
		request.addHeader(key, value);
		assertEquals(value, request.getHeader(key, "This should not display"));
	}

	@Test(expected=BadRequestException.class)
	public void shouldThrowBadRequestExceptionOnMissingUrlDecodedHeader()
	{
		request.getHeader("missing", "missing header");
	}
	
	@Test
	public void shouldBeGetRequest()
	{
		assertEquals(HttpMethod.GET, request.getHttpMethod());
		assertEquals(HttpMethod.GET, request.getEffectiveHttpMethod());
	}
	
	@Test
	public void shouldBePostRequest()
	{
		Request postRequest = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/foo"), null);
		assertEquals(HttpMethod.POST, postRequest.getHttpMethod());
		assertEquals(HttpMethod.POST, postRequest.getEffectiveHttpMethod());
	}

	@Test
	public void shouldBePutRequest()
	{
		Request putRequest = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.PUT, "/foo"), null);
		assertEquals(HttpMethod.PUT, putRequest.getHttpMethod());
		assertEquals(HttpMethod.PUT, putRequest.getEffectiveHttpMethod());
	}
	
	@Test
	public void shouldBeDeleteRequest()
	{
		Request deleteRequest = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.DELETE, "/foo"), null);
		assertEquals(HttpMethod.DELETE, deleteRequest.getHttpMethod());
		assertEquals(HttpMethod.DELETE, deleteRequest.getEffectiveHttpMethod());
	}

	@Test
	public void shouldBeEffectivePutRequest()
	{
		Request putRequest = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/foo?_method=pUt"), null);
		assertEquals(HttpMethod.POST, putRequest.getHttpMethod());
		assertEquals(HttpMethod.PUT, putRequest.getEffectiveHttpMethod());
	}

	@Test
	public void shouldBeEffectiveDeleteRequest()
	{
		Request deleteRequest = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/foo?_method=DeLeTe"), null);
		assertEquals(HttpMethod.POST, deleteRequest.getHttpMethod());
		assertEquals(HttpMethod.DELETE, deleteRequest.getEffectiveHttpMethod());
	}

	@Test
	public void shouldBeEffectivePostRequest()
	{
		Request deleteRequest = new Request(new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/foo?_method=xyzt"), null);
		assertEquals(HttpMethod.POST, deleteRequest.getHttpMethod());
		assertEquals(HttpMethod.POST, deleteRequest.getEffectiveHttpMethod());
	}

	@Test
	public void shouldParseUrlFormEncodedBody()
	throws Exception
	{
		String formValue1 = "http://login.berlin.ecollege-labs.com/google-service/google/sso/callback/google.JSON?successUrl=http%3A%2F%2Fdashboard.berlin.ecollege-labs.com%2Ftransfer.html&failureUrl=http%3A%2F%2Flogin.berlin.ecollege-labs.com&domain=GOOGLE_NON_MARKET_PLACE_DOMAIN";
		String formValue2 = "https://www.google.com/accounts/o8/id?id=AItOawkHDpeMEfe_xM14z_ge7UATYOSg_QlPeDg";
		String formValue3 = "https://www.google.com/accounts/o8/id?id=AItOawkHDpeMEfe_xM14z_ge7UATYOSg_QlPeDg";
		DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/foo?_method=xyzt", Unpooled.wrappedBuffer(("openid.return_to=" + URLEncoder.encode(formValue1, ContentType.ENCODING)
				+ "&openid.identity=" + URLEncoder.encode(formValue2, ContentType.ENCODING)
				+ "&openid.claimed_id=" + URLEncoder.encode(formValue3, ContentType.ENCODING)).getBytes()));
		Request formPost = new Request(httpRequest, null);
		Map<String, List<String>> form = formPost.getBodyFromUrlFormEncoded();
		assertEquals(3, form.size());
		assertNotNull(form.get("openid.return_to"));
		assertNotNull(form.get("openid.identity"));
		assertNotNull(form.get("openid.claimed_id"));
		assertEquals(formValue1, form.get("openid.return_to").get(0));
		assertEquals(formValue2, form.get("openid.identity").get(0));
		assertEquals(formValue3, form.get("openid.claimed_id").get(0));
	}

	@Test
	public void shouldHandleNullUrlFormEncodedBody()
	throws Exception
	{
		DefaultFullHttpRequest httpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, "/foo?_method=xyzt");
		Request formPost = new Request(httpRequest, null);
		Map<String, List<String>> form = formPost.getBodyFromUrlFormEncoded();
		assertEquals(0, form.size());
		form = formPost.getBodyFromUrlFormEncoded(true);
		assertEquals(0, form.size());
	}

	@Test
	public void shouldGetRequestHeaderNames()
	{
		request.addHeader("header-key", "header-value");
		request.addHeader("header-key-1", "header-value-1");
		request.addHeader("header-key-2", "");
		assertTrue(request.getHeaderNames().contains("header-key"));
		assertTrue(request.getHeaderNames().contains("header-key-1"));
		assertTrue(request.getHeaderNames().contains("header-key-2"));
	}
	
	@Test
	public void shouldGetAllRawHeadersWithSameName() {
		request.addHeader("common-key", "header-value");
		request.addHeader("common-key", "header-value-1");
		assertTrue(request.getHeaders("common-key").contains("header-value"));
		assertTrue(request.getHeaders("common-key").contains("header-value-1"));
	}

    @Test
    public void shouldNotReturnNullWhenNoQueryString() {
        Request noQueryRequest = new Request(
                new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.GET, "/noquery"),
                null, null
        );
        assertNotNull(noQueryRequest.getQueryStringMap());
    }
}
