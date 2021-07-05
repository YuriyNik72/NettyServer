package netty;

import handlers.ChatMessageHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;


public class NettyBaseServer {
    public NettyBaseServer() {
        EventLoopGroup auth = new NioEventLoopGroup(1); //light
        EventLoopGroup works = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();

            bootstrap.group(auth, works)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<>() {
                        @Override
                        protected void initChannel(Channel channel) throws Exception {
                            channel.pipeline().addLast(
                                    new StringDecoder(),
                                    new StringEncoder(),
                                    new ChatMessageHandler()
                            );
                        }
                    });
            ChannelFuture future = bootstrap.bind(5000).sync();
            System.out.println("Server started");
            future.channel().closeFuture().sync();
            System.out.println("Server finished");
        }catch (Exception e){
            e.printStackTrace();
        }finally {
         auth.shutdownGracefully();
         works.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new NettyBaseServer();
    }
}