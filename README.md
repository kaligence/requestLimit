# requestLimit

----------
### 简介
一款基于 redis 和 spring-aop 的请求限制工具，可以是实现简单的 ***单位时间内限制接口的请求次数***.

### 安装

 - maven
```
<dependency>
    <groupId>com.github.kaligence</groupId>
    <artifactId>requestLimit</artifactId>
    <version>{requestLimit.version}</version>
</dependency>
```

### 使用
1. 通过 maven 集成。
2. 工具依赖 redis 的配置，redis.xml 和 redis.properties 请务必使用eg文件夹内的示例 (由于代码跟xml内bean的name相对应)。
3. 工具源代码中使用了 @AspectJ 定义了一个切面实现相关功能，需要在 springMVC 的配置文件中添加如下代码使其生效，“proxy-target-class”是两套动态代理实现的开关，具体请看 [http://www.cnblogs.com/hustyangli/archive/2008/09/01/1281319.html](http://www.cnblogs.com/hustyangli/archive/2008/09/01/1281319.html)
```
<aop:aspectj-autoproxy proxy-target-class="true"/>
```
4. 同时需要在 springMVC 的配置文件中添加以下标签扫描一下工具类的目录。
```
<context:component-scan base-package="com.github.kaligence.requestLimit"/>
```
5. 工具的使用很简单，只需要在接口前调用自定义注解，示例:
```
@RequestLimit(requestCount = 1,requestTime = 10)
@ResponseBody
@RequestMapping("/checkversion")
public JSONObject checkversion() throws Exception {
    ...
}
```
 ***@RequestLimit***  就是自定义注解，目前包含3个可配置参数：

参数 | 详情 | 默认
-|-|-
requestCount|请求次数限制 (次)|1
requestTime|请求限制单位时间 (s)|60
debug|调试模式|false

6. 本工具默认接口返回 JSON 格式的数据，在未达到访问次数限制之前，返回的是该接口原本的返回值， ***在达到访问次数限制之后*** ，返回的则是自定义注解中定义的返回值 ***{"limit"： true}*** 表示已达到限制。


### 最后
希望有大佬看了我的实现之后如果有更好的方式，或者整个项目的各个地方有什么意见都可以提出来，大家一起探讨。