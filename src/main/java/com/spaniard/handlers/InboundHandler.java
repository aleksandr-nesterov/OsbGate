package com.spaniard.handlers;

import com.spaniard.ApplicationProperties;
import com.spaniard.MappingProperties;
import com.spaniard.model.RequestData;
import com.spaniard.model.RequestStatement;
import com.spaniard.model.Url;
import com.spaniard.DynamicMappingHolder;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Alexander Nesterov
 * @version 1.0
 */
public class InboundHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(InboundHandler.class);

    private static final String HOST = ApplicationProperties.getInstance().getOsbHost();
    private static final int PORT = ApplicationProperties.getInstance().getOsbPort();

    private HttpRequest request;

    private final StringBuilder requestContent = new StringBuilder();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.channel().writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            this.request = (HttpRequest) msg;
        }
        if (msg instanceof HttpContent) {
            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();
            if (content.isReadable()) {
                requestContent.append(content.toString(CharsetUtil.UTF_8));
            }
            if (msg instanceof LastHttpContent) {
                RequestData requestData;
                if ((requestData = isDynamicMappingRequest(request)) != null) {
                    final FullHttpResponse response = updateDynamicMapping(requestData);
                    ctx.writeAndFlush(response);
                } else {
                    RequestStatement requestStatement;
                    // 1
                    if ((requestStatement = searchForDynamicMapping(request)) == null) {
                        LOG.debug("No DynamicMapping found for uri [{}]", request.uri());
                        // 2
                        if ((requestStatement = searchForStaticMapping(request)) == null) {
                            LOG.debug("No StaticMapping found for uri [{}]", request.uri());
                            // 3
                            requestStatement = osbMapping(request);
                        }
                    }

                    LOG.debug(requestStatement.toString());
                    writeAndFlush(ctx, requestStatement);

                    // delete prev content
                    requestContent.delete(0, requestContent.length());
                }

            }
        }
    }

    private RequestData isDynamicMappingRequest(HttpRequest request) {
        final char[] arr = request.uri().toCharArray();
        final StringBuilder builder = new StringBuilder();
        String path = null;
        int i = 0;
        for (; i < arr.length; i++) {
            if (arr[i] == '?') {
                path = builder.toString();
                builder.delete(0, builder.length());
                break;
            } else {
                builder.append(arr[i]);
            }
        }
        if (path == null) path = builder.toString();
        final Map<String, String> params = getParameters(i);
        switch (path) {
            case DynamicMappingHolder.SET_MAPPING:
            case DynamicMappingHolder.CLEAR_MAPPING:
            case DynamicMappingHolder.REMOVE_KEY:
                return new RequestData(path, params);
            default:
                return null;
        }
    }

    private FullHttpResponse updateDynamicMapping(final RequestData requestData) {
        switch (requestData.getUri()) {
            case DynamicMappingHolder.SET_MAPPING:
                Map<String, String> params = requestData.getParams();
                if (validateParameters(requestData.getParams())) {
                    return createHttpResponse("Invalid parameters given");
                }
                final Url url = new Url(params.get(DynamicMappingHolder.HOST_KEY), Integer.valueOf(params.get(DynamicMappingHolder.PORT_KEY)), params.get(DynamicMappingHolder.PATH_KEY));
                LOG.debug("Update dynamic mapping. Key: [{}], Value: [{}]", params.get(DynamicMappingHolder.KEY), url);
                DynamicMappingHolder.dynamicMapping.put(params.get(DynamicMappingHolder.KEY), url);
                break;
            case DynamicMappingHolder.CLEAR_MAPPING:
                LOG.debug("Clear dynamic mapping");
                DynamicMappingHolder.dynamicMapping.clear();
                break;
            case DynamicMappingHolder.REMOVE_KEY:
                params = requestData.getParams();
                LOG.debug("Remove dynamic mapping. Key: [{}]", params.get(DynamicMappingHolder.KEY));
                DynamicMappingHolder.dynamicMapping.remove(params.get(DynamicMappingHolder.KEY));
                break;
        }
        return createHttpResponse("Done");
    }

    private FullHttpResponse createHttpResponse(final String message) {
        final FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.copiedBuffer(message, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        response.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());
        return response;
    }

    private boolean validateParameters(Map<String, String> params) {
        return params.get(DynamicMappingHolder.HOST_KEY) == null || params.get(DynamicMappingHolder.PORT_KEY) == null || params.get(DynamicMappingHolder.PATH_KEY) == null;
    }

    private Map<String, String> getParameters(final int index) {
        final Map<String, String> params = new HashMap<>();
        final char[] arr = request.uri().toCharArray();
        final StringBuilder builder = new StringBuilder();
        String key = null;
        for (int i = index + 1; i < arr.length; i++) {
            if (arr[i] == '=') {
                key = builder.toString();
                builder.delete(0, builder.length());
            } else if (arr[i] == '&') {
                final String value = builder.toString();
                params.put(key, value);
                builder.delete(0, builder.length());
            } else {
                builder.append(arr[i]);
            }
        }
        final String value = builder.toString();
        params.put(key, value);
        return params;
    }

    private RequestStatement searchForDynamicMapping(final HttpRequest request) {
        final Url url = DynamicMappingHolder.dynamicMapping.get(request.uri());
        if (url != null) {
            LOG.debug("Found dynamic mapping [{}]", url);
        }
        return url != null ? createRequestStatement(url.getHost(), url.getPort(), url.getUri()) : null;
    }

    private RequestStatement searchForStaticMapping(final HttpRequest request) {
        final String uri = request.uri();
        final Url url = MappingProperties.getInstance().getUrl(uri);
        if (url != null) {
            LOG.debug("Found static mapping [{}]", url);
        }
        return url != null ? createRequestStatement(url.getHost(), url.getPort(), url.getUri()) : null;
    }

    private RequestStatement osbMapping(final HttpRequest request) {
        final String uri = request.uri();
        return createRequestStatement(HOST, PORT, uri);
    }

    private RequestStatement createRequestStatement(final String host, final int port, final String uri) {
        // Prepare the HTTP request.
        final HttpRequest httpRequest = new DefaultFullHttpRequest(HTTP_1_1, HttpMethod.POST, uri,
                Unpooled.copiedBuffer(requestContent.toString().getBytes(CharsetUtil.UTF_8)));

        httpRequest.headers().set(HttpHeaderNames.HOST, host + ":" + port);
        httpRequest.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        httpRequest.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/xml; charset=utf-8");
        httpRequest.headers().set(HttpHeaderNames.TRANSFER_ENCODING, "chunked");
        final Url url = new Url(host, port, uri);
        return new RequestStatement(url, httpRequest);
    }


    private void writeAndFlush(final ChannelHandlerContext ctx, final RequestStatement requestStatement) {
        final Channel inboundChannel = ctx.channel();
        final Bootstrap b = new Bootstrap();
        b.group(inboundChannel.eventLoop())
                .channel(ctx.channel().getClass())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new HttpClientCodec());
                        p.addLast(new OutboundHandler(inboundChannel));
                    }
                }).option(ChannelOption.AUTO_READ, false);
        final ChannelFuture f = b.connect(requestStatement.getUrl().getHost(), requestStatement.getUrl().getPort());
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) {
                    // connection complete write and flush request
                    f.channel().writeAndFlush(requestStatement.getHttpRequest());
                } else {
                    // Close the connection if the connection attempt has failed.
                    inboundChannel.close();
                }
            }
        });
    }
}
