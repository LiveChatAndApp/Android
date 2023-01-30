# 延伸的 野火IM解决方案

有興趣可以到 野火團隊 去 下載 代碼  https://github.com/wildfirechat

這邊是 用於學習方面 野火沒有給的 admin 管理介面和 ui 對於 程序員 教學 或是 學習 使用

此版本已經驗證 



## 延伸版本 野火
主要包括以下项目：

| [GitHub仓库地址](https://github.com/LiveChatAndApp)       | 说明                                                                                      
| ------------------------------------------------------------  | --------------------------------------------------------------------------
| [im-server](https://github.com/LiveChatAndApp/im-server)          | 野火社区版IM服务，野火IM的核心服务，处理所有IM相关业务。  |
| [app_server](https://github.com/LiveChatAndApp/app_server)       | Demo应用服务，模拟客户的应用服登陆处理逻辑及部分二次开发示例。 |
| [admin-ui](https://github.com/LiveChatAndApp/admin-ui)       | Demo应用服务，基於vue admin element 的 admin 管理介面。 |
| [admin-api](https://github.com/LiveChatAndApp/im-admin)       | Demo应用服务，admin 後台 api 開發。 |
| [android-chat](https://github.com/LiveChatAndApp/Android) | 野火IM Android SDK源码和App源码。 |
| [ios-chat](https://github.com/LiveChatAndApp/ios)             | 野火IM iOS SDK源码和App源码。|



## 开发调试说明

我们采用最新稳定版Android Studio及对应的gradle进行开发，对于旧版本的IDE，我们没有测试，编译之类问题，需自行解决。

## 二次开发说明

野火IM采用bugly作为日志手机工具，大家二次开发时，务必将```MyApp.java```中的 ```bugly id``` 替换为你们自己的，否则错误日志都跑我们这儿来了，你们收集不到错误日志，我们也会受到干扰。

## 混淆说明
1. 确保所依赖的```lifecycle```版本在2.2.0或以上。
2. 参考```chat/proguard-rules.pro```进行配置。

## Android Support 说明

野火IM Android 客户端，基于```AndroidX```包开发，如果老项目采用的是```Android Support```包，可尝试采用[jetifier](https://developer.android.google.cn/studio/command-line/jetifier?hl=zh_cn)
转成```Android Support```软件包。

## Android 4.x 说明
请使用[api-19](https://github.com/wildfirechat/android-chat/tree/api-19)分支，如果编译失败等，可能是4.x版本的协议栈版本没有及时更新所导致，请微信联系 wfchat 进行更新。

## 升级注意
v0.8.0 版本，对代码结构及部分实现机制进行了大量调整，变动如下：

1. 将```chat``` application module 拆分为两部分：```uikit``` library module 和 ```chat``` application module。```uikit```可以library的方式导入项目，里面包含了大量可重用的UI。
2. 移除```LayoutRes```、```SendLayoutRes```、```ReceiveLayoutRes```等注解，并更新```MessageViewHolder```等的实现机制

## 特别注意
1. ```com.android.tools.build:gradle:3.5.0``` 可能存在bug，会导致音视频crash，请勿使用此版本

### 联系我们


1. 邮箱: jchaintw@gmail.com 


## 集成
1. client部分，自行下载代码，并将client module引入你们自己的项目。
2. uikit部分，自行下载代码，并将uikit module引入你们自己的项目。
3. push部分，自行下载代码，将push module引入你们自己的项目。


## 贡献
欢迎提交pull request，一起打造一个更好的开源IM。

## 鸣谢
1. [LQRWeChat](https://github.com/GitLqr/LQRWeChat) 本项目中图片选择器、表情基于此开发
2. [butterKnife](https://github.com/JakeWharton/butterknife)
3. OKHttp等一些其他优秀的开源项目
4. 本工程使用的Icon全部来源于[icons8](https://icons8.com)，对他们表示感谢。
5. Gif动态图来源于网络，对网友的制作表示感谢。

如果有什么地方侵犯了您的权益，请联系我们删除🙏🙏🙏

## License

1. Under the Creative Commons Attribution-NoDerivs 3.0 Unported license. See the [LICENSE](https://github.com/wildfirechat/android-chat/blob/master/LICENSE) file for details.
