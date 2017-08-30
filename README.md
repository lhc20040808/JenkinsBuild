# JenkinsBuild
本文简单记录一下搭建自动化构建过程中遇到的各种坑

服务器：阿里云 centos7  内存1GB

首先下载安装jdk，android sdk，gradle，git把各种该配的环境变量配置好。网上各种教程一大堆不再赘述了。唯一需要注意的是这些东西安装好之后找个简单的项目编译(gradle assembleRelease/assembleDebug)一下看看有没有问题。以免在持续构建中出现问题无法定位。



#### apk签名

这里采用的方式是在文件目录下新建一个signings.properties，将keystore信息写入其中，最后通过脚本注入。



```groovy
android {
  
	//省略部分配置
	
    signingConfigs {

        release {
            storeFile
            storePassword
            keyAlias
            keyPassword
        }

    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            buildConfigField "String", "MARKET_URL", "\"${MARKET_URL}\""
            signingConfig signingConfigs.release
        }

        debug {
            buildConfigField "String", "MARKET_URL", "\"${MARKET_URL}\""
            signingConfig signingConfigs.release
        }
    }

    lintOptions {
        abortOnError false//lint时候终止错误上报,防止编译的时候莫名的失败
    }

    //配置打包签名
    File propFile = file('../signing.properties');
    if (propFile.exists()) {
        def Properties props = new Properties()
        props.load(new FileInputStream(propFile))
        if (props.containsKey('storeFileName') && props.containsKey('storePassword') &&
                props.containsKey('keyAlias') && props.containsKey('keyPassword')) {
            android.signingConfigs.release.storeFile = file(props['storeFileName'])
            android.signingConfigs.release.storePassword = props['keyPassword']
            android.signingConfigs.release.keyAlias = props['keyAlias']
            android.signingConfigs.release.keyPassword = props['keyPassword']
        } else {
            android.buildTypes.release.signingConfig = null
        }
    } else {
        android.buildTypes.release.signingConfig = null
    }
}
```

```
storeFileName=/usr/local/data/lhc.keystore
storePassword=112233
keyAlias=lhc
keyPassword=112233
```



#### 可变参数String Parameter构建的坑

在jenkins中我添加了一个MARKET_URL参数，这个值申明在文件`gradle.properties`中。编译过程提换这个值，然而String parameter似乎只做简单的替换，并不区别类型。

个人感觉这跟groovy的语法有关，印象里在groovy中由编译器来判断变量类型，当采用第一种错误写法，如果构建的时候MARKET_URL给的是123，由于会将123直接赋值给String类型的MARKET_URL变量，构建过程会报错。当然，也可以在构建的时候把这个变量值写成"123"，这样就能编译通过且正确赋值，但感觉不够友好。

错误写法

```groovy
buildConfigField "String", "MARKET_URL", MARKET_URL
```

正确写法

```groovy
buildConfigField "String", "MARKET_URL", "\"${MARKET_URL}\""
```



#### 守护进程的坑

在linux下通过gradle命令编译能成功，之后在jenkins构建中依然出现了错误，编译到一半莫名其妙中断了（错误日志里面就是这么写的...），查了一下有说是因为服务器内存不够（这也是我要提我服务器内存的原因），但普遍矛头指向gradle编译过程中的守护进程，因为我不是人民币玩家也没办法尝试是不是升级内存之后会好。这里采用了关闭守护进程的方法后编译通过。

在`gradle.properties`加入`org.gradle.daemon=false`

以下是我gradle.properties的内容

```groovy
org.gradle.jvmargs=-Xmx1536m
org.gradle.daemon=false
MARKET_URL = ""
```



#### 编译好后apk下载的坑

这个问题主要是因为我对filezilla这个软件不够熟悉，下载文件和下载到的目录都需要有权限才能成功下载。当然还有防火墙端口，阿里云安全组配置的各种坑，这里不赘述了，踩过的都懂。



参考链接：[enkins+Gradle+Git+Centos 实现android持续集成、打包（超详细）](http://www.jianshu.com/p/5feca12a2ada)

