package org.voovan.http.server;

import org.voovan.http.monitor.Monitor;
import org.voovan.http.server.websocket.WebSocketBizHandler;
import org.voovan.http.server.websocket.WebSocketDispatcher;
import org.voovan.network.SSLManager;
import org.voovan.network.aio.AioServerSocket;
import org.voovan.network.messagesplitter.HttpMessageSplitter;
import org.voovan.tools.log.Logger;

import java.io.IOException;
import java.text.ParseException;

/**
 * HttpServer 对象
 * 
 * @author helyho
 * 
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
public class HttpServer {
	private AioServerSocket		aioServerSocket;
	private HttpDispatcher	httpDispatcher;
	private WebSocketDispatcher webSocketDispatcher;
	private SessionManager sessionManager;
	private WebServerConfig config;

	/**
	 * 构造函数
	 * 
	 * @param config
	 * @throws IOException
	 *             异常
	 */
	public HttpServer(WebServerConfig config) throws IOException {
		this.config = config;

		// 准备 socket 监听
		aioServerSocket = new AioServerSocket(config.getHost(), config.getPort(), config.getTimeout()*1000);

		//构造 SessionManage
		sessionManager = SessionManager.newInstance(config);

		//请求派发器创建
		this.httpDispatcher = new HttpDispatcher(config,sessionManager);

		this.webSocketDispatcher = new WebSocketDispatcher(config);

		//确认是否启用 HTTPS 支持
		if(config.getCertificateFile()!=null) {
			SSLManager sslManager = new SSLManager("TLS", false);
			sslManager.loadCertificate(System.getProperty("user.dir") + config.getCertificateFile(),
					config.getCertificatePassword(), config.getKeyPassword());
			aioServerSocket.setSSLManager(sslManager);
		}

		aioServerSocket.handler(new HttpServerHandler(config, httpDispatcher,webSocketDispatcher));
		aioServerSocket.filterChain().add(new HttpServerFilter());
		aioServerSocket.messageSplitter(new HttpMessageSplitter());

		//初始化并安装监控功能
		if(config.isMonitor()){
			Monitor.installMonitor(this);
		}
	}

	/**
	 * 获取配置对象
	 * @return
     */
	public WebServerConfig getWebServerConfig() {
		return config;
	}

	/**
	 * 以下是一些 HTTP 方法的成员函数
	 */

	public void get(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("GET", routeRegexPath, handler);
	}

	public void post(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("POST", routeRegexPath, handler);
	}

	public void head(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("HEAD", routeRegexPath, handler);
	}

	public void put(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("PUT", routeRegexPath, handler);
	}

	public void delete(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("delete", routeRegexPath, handler);
	}

	public void trace(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("TRACE", routeRegexPath, handler);
	}

	public void connect(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("CONNECT", routeRegexPath, handler);
	}

	public void options(String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteHandler("OPTIONS", routeRegexPath, handler);
	}

	public void otherMethod(String method, String routeRegexPath, HttpBizHandler handler) {
		httpDispatcher.addRouteMethod(method);
		httpDispatcher.addRouteHandler(method, routeRegexPath, handler);
	}
	
	public void socket(String routeRegexPath, WebSocketBizHandler handler) {
		webSocketDispatcher.addRouteHandler(routeRegexPath, handler);
	}
	

	/**
	 * 构建新的 HttpServer,从配置文件读取配置
	 * 
	 * @return
	 */
	public static HttpServer newInstance() {
		try {
			return new HttpServer(WebContext.getWebServerConfig());
		} catch (IOException e) {
			Logger.error("Create HttpServer failed.",e);
		}
		return null;
	}

	/**
	 * 启动服务
	 * 
	 * @throws IOException
	 */
	public void serve() throws IOException {
		WebContext.welcome(config);
		aioServerSocket.start();

	}
}
