# [Spring-Boot2-Admin](https://github.com/happyflyer/Spring-Boot2-Admin)

- [SpringBoot2 零基础入门](https://www.bilibili.com/video/BV19K4y1L7MT)
- [SpringBoot2 核心技术与响应式编程](https://www.yuque.com/atguigu/springboot)

## 1. SpringMVC 自动配置概览

- 简单功能分析
- 请求参数处理
- 数据响应与内容协商
- 视图解析与模板引擎
- 拦截器
- 跨域
- 异常处理
- 原生 Servlet 组件
- 嵌入式 Web 容器
- 定制化原理

## 2. 拦截器

### 2.1. HandlerInterceptor 接口

```java
package org.springframework.web.servlet;
public interface HandlerInterceptor {
    default boolean preHandle(HttpServletRequest request,
                              HttpServletResponse response,
                              Object handler) throws Exception {
        return true;
    }
    default void postHandle(HttpServletRequest request,
                            HttpServletResponse response,
                            Object handler,
                            @Nullable ModelAndView modelAndView) throws Exception {
    }
    default void afterCompletion(HttpServletRequest request,
                                 HttpServletResponse response,
                                 Object handler,
                                 @Nullable Exception ex) throws Exception {
    }
}
```

- 目标方法执行之前 `preHandle`
- 目标方法执行之后 `postHandle`
- 页面渲染之后 `afterCompletion`

### 2.2. 配置拦截器

1. 配置好拦截器要拦截哪些请求
2. 把这些配置放在容器中

- 编写一个拦截器实现 `HandlerInterceptor` 接口
- 拦截器注册到容器中，实现 `WebMvcConfigurer` 的 `addInterceptors`
- 指定拦截规则，如果是拦截所有，静态资源也会被拦截

```java
@Slf4j
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        log.info("xxxx执行拦截器方法: {}.{}", "LoginInterceptor", "preHandle()");
        String requestUri = request.getRequestURI();
        // 登录检查逻辑
        HttpSession session = request.getSession();
        Object loginUser = session.getAttribute("loginUser");
        if (loginUser != null) {
            log.info("--->放行的请求路径是: {}", requestUri);
            return true;
        }
        log.info("-|->拦截的请求路径是: {}", requestUri);
        request.setAttribute("msg", "请先登录");
        request.getRequestDispatcher("/login").forward(request, response);
        return false;
    }
    @Override
    public void postHandle(HttpServletRequest request,
                           HttpServletResponse response,
                           Object handler,
                           ModelAndView modelAndView) throws Exception {
        log.info("xxxx执行拦截器方法: {}.{}", "LoginInterceptor", "postHandle()");
    }
    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) throws Exception {
        log.info("xxxx执行拦截器方法: {}.{}", "LoginInterceptor", "afterCompletion()");
    }
}
```

```java
@Configuration
public class AdminWebConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/error")
                .excludePathPatterns("/", "/login")
                .excludePathPatterns("/css/**", "/fonts/**", "/images/**", "/js/**")
                .excludePathPatterns("/static/**");
    }
}
```

```yml
spring:
  mvc:
    static-path-pattern: /static/**
```

### 2.3. 拦截器原理

```java
package org.springframework.web.servlet;
public class DispatcherServlet extends FrameworkServlet {
    // ...
    protected void doDispatch(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;
        // ...
        // Determine handler for the current request.
        mappedHandler = getHandler(processedRequest);
        // ...
        // Determine handler adapter for the current request.
        HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());
        // ...
        // 关键之一、执行拦截器的 preHandle 方法
        if (!mappedHandler.applyPreHandle(processedRequest, response)) {
            return;
        }
        // Actually invoke the handler.
        mv = ha.handle(processedRequest, response, mappedHandler.getHandler());
        // ...
        // 关键之二、执行拦截器的 postHandle 方法
        mappedHandler.applyPostHandle(processedRequest, response, mv);
        // ...
        // 关键之三、执行拦截器的 afterCompletion 方法
        // 执行处理器方法、渲染视图过程不发生异常，在渲染视图之后，执行拦截器的 afterCompletion 方法
        processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
        // 在任何部位发生异常，就立即结束请求处理过程，并执行拦截器的 afterCompletion 方法
        triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
        // ...
    }
    // ...
    private void processDispatchResult(HttpServletRequest request,
                                       HttpServletResponse response,
                                       @Nullable HandlerExecutionChain mappedHandler,
                                       @Nullable ModelAndView mv,
                                       @Nullable Exception exception) throws Exception {
        // ...
        if (mappedHandler != null) {
            mappedHandler.triggerAfterCompletion(request, response, null);
        }
    }
    // ...
    private void triggerAfterCompletion(HttpServletRequest request,
                                        HttpServletResponse response,
                                        @Nullable HandlerExecutionChain mappedHandler,
                                        Exception ex) throws Exception {
        if (mappedHandler != null) {
            mappedHandler.triggerAfterCompletion(request, response, ex);
        }
        throw ex;
    }
    // ...
}
```

```java
package org.springframework.web.servlet;
public class HandlerExecutionChain {
    // ...
    boolean applyPreHandle(HttpServletRequest request,
                           HttpServletResponse response) throws Exception {
        HandlerInterceptor[] interceptors = getInterceptors();
        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = 0; i < interceptors.length; i++) {
                HandlerInterceptor interceptor = interceptors[i];
                if (!interceptor.preHandle(request, response, this.handler)) {
                    triggerAfterCompletion(request, response, null);
                    return false;
                }
                this.interceptorIndex = i;
            }
        }
        return true;
    }
    // ...
    void applyPostHandle(HttpServletRequest request,
                         HttpServletResponse response,
                         @Nullable ModelAndView mv) throws Exception {
        HandlerInterceptor[] interceptors = getInterceptors();
        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = interceptors.length - 1; i >= 0; i--) {
                HandlerInterceptor interceptor = interceptors[i];
                interceptor.postHandle(request, response, this.handler, mv);
            }
        }
    }
    // ...
    void triggerAfterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                @Nullable Exception ex) throws Exception {
        HandlerInterceptor[] interceptors = getInterceptors();
        if (!ObjectUtils.isEmpty(interceptors)) {
            for (int i = this.interceptorIndex; i >= 0; i--) {
                HandlerInterceptor interceptor = interceptors[i];
                try {
                    interceptor.afterCompletion(request, response, this.handler, ex);
                }
                catch (Throwable ex2) {
                    logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);
                }
            }
        }
    }
    // ...
}
```

1. 根据当前请求，找到 `HandlerExecutionChain`【可以处理请求的 handler 以及 handler 的所有 拦截器】
2. 先来**顺序执行** 所有拦截器的 `preHandle` 方法
   1. 如果当前拦截器 `preHandle` 返回为 `true`。则执行下一个拦截器的 `preHandle`
   2. 如果当前拦截器返回为 `false`。直接 **倒序执行**所有已经执行了的拦截器的 `afterCompletion`
3. 如果任何一个拦截器返回 `false`。直接跳出不执行目标方法
4. 所有拦截器都返回 `true`。执行目标方法
5. 倒序执行所有拦截器的 `postHandle` 方法
6. 前面的步骤有任何异常都会直接**倒序**触发 `afterCompletion`
7. 页面成功渲染完成以后，也会**倒序**触发 `afterCompletion`

## 3. 文件上传

### 3.1. MultipartFile 使用

```html
<form role="form" th:action="@{/upload}" method="post" enctype="multipart/form-data">
  <div class="form-group">
    <label for="exampleInputEmail1">电子邮箱</label>
    <input type="email" name="email" class="form-control" id="exampleInputEmail1" placeholder="Enter email" />
  </div>
  <div class="form-group">
    <label for="exampleInputPassword1">密码</label>
    <input type="password" name="password" class="form-control" id="exampleInputPassword1" placeholder="Password" />
  </div>
  <div class="form-group">
    <label for="exampleInputFile">单个文件上传</label>
    <input type="file" id="exampleInputFile" name="one_file" />
    <p class="help-block">Example block-level help text here.</p>
  </div>
  <div class="form-group">
    <label>多个文件上传</label>
    <input type="file" name="multi_file" multiple />
    <p class="help-block">Example block-level help text here.</p>
  </div>
  <div class="checkbox">
    <label> <input type="checkbox" /> Check me out </label>
  </div>
  <button type="submit" class="btn btn-primary">提交</button>
</form>
```

```java
@Slf4j
@Controller
public class FormController {
    @GetMapping("/form_layouts")
    public String formLayout() {
        return "form/form_layouts";
    }
    @PostMapping("/upload")
    public String upload(@RequestParam("email") String email,
                         @RequestParam("password") String password,
                         @RequestParam("one_file") MultipartFile oneFile,
                         @RequestParam("multi_file") MultipartFile[] multiFile)
            throws IOException {
        log.info("cccc上传的数据：email={}, password={}, oneFile={}, multiFile={}",
                email, password, oneFile.getSize(), multiFile.length);
        if (!oneFile.isEmpty()) {
            String originalFilename = oneFile.getOriginalFilename();
            oneFile.transferTo(new File("D:/" + originalFilename));
        }
        if (multiFile.length > 0 && new File("D:/cache/").mkdirs()) {
            for (MultipartFile file : multiFile) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    file.transferTo(new File("D:/cache/" + originalFilename));
                }
            }
        }
        return "main";
    }
}
```

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 100MB
```

### 3.2. 文件上传解析器

```java
package org.springframework.boot.autoconfigure.web.servlet;
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({
    Servlet.class,
    StandardServletMultipartResolver.class,
    MultipartConfigElement.class
})
@ConditionalOnProperty(prefix = "spring.servlet.multipart", name = "enabled", matchIfMissing = true)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(MultipartProperties.class)
public class MultipartAutoConfiguration {
    private final MultipartProperties multipartProperties;
    // ...
    @Bean
    @ConditionalOnMissingBean({ MultipartConfigElement.class, CommonsMultipartResolver.class })
    public MultipartConfigElement multipartConfigElement() {
        return this.multipartProperties.createMultipartConfig();
    }
    @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)
    @ConditionalOnMissingBean(MultipartResolver.class)
    public StandardServletMultipartResolver multipartResolver() {
        StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
        // ...
        return multipartResolver;
    }
}
```

- `MultipartAutoConfiguration` 文件上传自动配置类
  - `MultipartProperties` 文件上传参数配置
  - `StandardServletMultipartResolver` 文件上传解析器

```java
package org.springframework.boot.autoconfigure.web.servlet;
@ConfigurationProperties(prefix = "spring.servlet.multipart", ignoreUnknownFields = false)
public class MultipartProperties {
    private boolean enabled = true;
    private String location;
    private DataSize maxFileSize = DataSize.ofMegabytes(1);
    private DataSize maxRequestSize = DataSize.ofMegabytes(10);
    private DataSize fileSizeThreshold = DataSize.ofBytes(0);
    private boolean resolveLazily = false;
    // ...
}
```

```java
package javax.servlet;
public class MultipartConfigElement {
    private final String location;// = "";
    private final long maxFileSize;// = -1;
    private final long maxRequestSize;// = -1;
    private final int fileSizeThreshold;// = 0;
    // ...
}
```

```java
package org.springframework.web.multipart.support;
public class StandardServletMultipartResolver implements MultipartResolver {
    private boolean resolveLazily = false;
    // ...
    @Override
    public boolean isMultipart(HttpServletRequest request) {
        return StringUtils.startsWithIgnoreCase(request.getContentType(), "multipart/");
    }
    @Override
    public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request)
            throws MultipartException {
        return new StandardMultipartHttpServletRequest(request, this.resolveLazily);
    }
    // ...
}
```

```java
package org.springframework.web.multipart;
public interface MultipartResolver {
    boolean isMultipart(HttpServletRequest request);
    MultipartHttpServletRequest resolveMultipart(HttpServletRequest request)
            throws MultipartException;
    void cleanupMultipart(MultipartHttpServletRequest request);
}
```

### 3.3. 文件上传流程

```java
package org.springframework.web.servlet;
public class DispatcherServlet extends FrameworkServlet {
    // ...
    protected void doDispatch(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        HttpServletRequest processedRequest = request;
        HandlerExecutionChain mappedHandler = null;
        boolean multipartRequestParsed = false;
        // ...
        // 检查是否为文件上传请求，如果是，包装请求对象
        processedRequest = checkMultipart(request);
        multipartRequestParsed = (processedRequest != request);
        // ...
    }
    // ...
    protected HttpServletRequest checkMultipart(HttpServletRequest request)
            throws MultipartException {
        if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {
            // ...
            return this.multipartResolver.resolveMultipart(request);
            // ...
        }
        // If not returned before: return original request.
        return request;
    }
    // ...
}
```

```java
package org.springframework.web.servlet.mvc.method.annotation;
public class RequestPartMethodArgumentResolver
        extends AbstractMessageConverterMethodArgumentResolver {
    // ...
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // ...
    }
    @Override
    @Nullable
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest request,
                                  @Nullable WebDataBinderFactory binderFactory)
            throws Exception {
        // ...
    }
    // ...
}
```

```java
package org.springframework.web.multipart.support;
public final class MultipartResolutionDelegate {
    // ...
    @Nullable
    public static Object resolveMultipartArgument(String name,
                                                  MethodParameter parameter,
                                                  HttpServletRequest request)
            throws Exception {
        // ...
    }
    // ...
}
```

- 文件上传原理步骤
  - 请求进来使用文件上传解析器判断 `isMultipart`
  - 如果是，封装（`resolveMultipart`，返回 `MultipartHttpServletRequest`）文件上传请求
  - 参数解析器 `RequestPartMethodArgumentResolver` 来解析请求中的文件内容封装成 `MultipartFile`
  - 将 request 中文件信息封装为一个 Map，`MultiValueMap<String, MultipartFile>`
  - `FileCopyUtils` 实现文件流的拷贝

## 4. 异常处理

### 4.1. 默认处理机制

- 默认情况下，Spring Boot 提供 `/error` 处理所有错误的映射
- 对于机器客户端，它将生成 JSON 响应，其中包含错误，HTTP 状态和异常消息的详细信息
- 对于浏览器客户端，响应一个 Whitelabel 错误视图，以 HTML 格式呈现相同的数据
- 要对其进行自定义，添加 View 解析为 `error`
- 要完全替换默认行为，可以实现 `ErrorController` 并注册该类型的 Bean 定义
- 或添加 `ErrorAttributes` 类型的组件以使用现有机制但替换其内容
- `error/` 下的 `4xx`，`5xx` 页面会被自动解析

### 4.2. 自动配置原理

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class })
// Load before the main WebMvcAutoConfiguration so that the error View is available
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties({
    ServerProperties.class,
    ResourceProperties.class,
    WebMvcProperties.class
})
public class ErrorMvcAutoConfiguration {
    // ...
    // 关键之一：错误信息属性
    @Bean
    @ConditionalOnMissingBean(value = ErrorAttributes.class, search = SearchStrategy.CURRENT)
    public DefaultErrorAttributes errorAttributes() {
        return new DefaultErrorAttributes();
    }
    // 关键之二、错误处理控制器
    @Bean
    @ConditionalOnMissingBean(value = ErrorController.class, search = SearchStrategy.CURRENT)
    public BasicErrorController basicErrorController(ErrorAttributes errorAttributes,
            ObjectProvider<ErrorViewResolver> errorViewResolvers) {
        return new BasicErrorController(errorAttributes,
                this.serverProperties.getError(),
                errorViewResolvers.orderedStream().collect(Collectors.toList()));
    }
    // ...
    @Configuration(proxyBeanMethods = false)
    static class DefaultErrorViewResolverConfiguration {
        // ...
        // 关键之三、错误视图解析器
        @Bean
        @ConditionalOnBean(DispatcherServlet.class)
        @ConditionalOnMissingBean(ErrorViewResolver.class)
        DefaultErrorViewResolver conventionErrorViewResolver() {
            return new DefaultErrorViewResolver(this.applicationContext, this.resourceProperties);
        }
    }
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(
            prefix = "server.error.whitelabel",
            name = "enabled",
            matchIfMissing = true
    )
    @Conditional(ErrorTemplateMissingCondition.class)
    protected static class WhitelabelErrorViewConfiguration {
        private final StaticView defaultErrorView = new StaticView();
        // 关键之四、白页视图组件，id为error
        @Bean(name = "error")
        @ConditionalOnMissingBean(name = "error")
        public View defaultErrorView() {
            return this.defaultErrorView;
        }
        // 按组件id在容器中找View对象的视图解析器
        @Bean
        @ConditionalOnMissingBean
        public BeanNameViewResolver beanNameViewResolver() {
            BeanNameViewResolver resolver = new BeanNameViewResolver();
            // ...
            return resolver;
        }
    }
    // ...
}
```

- `ErrorMvcAutoConfiguration` 自动配置异常处理规则
  - 类型：`DefaultErrorAttributes` -> id：`errorAttributes`
    - 定义错误页面中可以包含哪些数据
  - 类型：`BasicErrorController` --> id：`basicErrorController`
    - 处理默认 `/error` 路径的请求
    - HTML 响应 `new ModelAndView("error", model);`
    - JSON 响应 `new ResponseEntity<>(body, status);`
  - 类型：`DefaultErrorViewResolver` -> id：`conventionErrorViewResolver`
    - 如果发生错误，会以 HTTP 的状态码 作为视图页地址（viewName），找到真正的页面
    - `error/viewName.html`
  - 类型：`StaticView` -> id：`error`
    - 如果想要返回页面；就会找 `error` 视图【`StaticView`】
    - 默认是一个白页

#### 4.2.1. 错误信息属性

```java
package org.springframework.boot.web.servlet.error;
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultErrorAttributes
        implements ErrorAttributes, HandlerExceptionResolver, Ordered {
    // ...
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                  ErrorAttributeOptions options) {
        Map<String, Object> errorAttributes = getErrorAttributes(
                webRequest, options.isIncluded(Include.STACK_TRACE));
        // exception
        // trace
        // message
        // errors
        return errorAttributes;
    }
    @Override
    @Deprecated
    public Map<String, Object> getErrorAttributes(WebRequest webRequest,
                                                  boolean includeStackTrace) {
        Map<String, Object> errorAttributes = new LinkedHashMap<>();
        // timestamp
        // status
        // path
        return errorAttributes;
    }
    // ...
}
```

#### 4.2.2. 错误处理控制器

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
@Controller
@RequestMapping("${server.error.path:${error.path:/error}}")
public class BasicErrorController extends AbstractErrorController {
    // ...
    // 响应白页
    @RequestMapping(produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        // ...
        // 如果错误视图解析器能能解析，就用错误视图解析器的结果
        // 如果不行，就用名为 error 的白页
        ModelAndView modelAndView = resolveErrorView(request, response, status, model);
        return (modelAndView != null) ? modelAndView : new ModelAndView("error", model);
    }
    // 响应JSON
    @RequestMapping
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        // ...
        return new ResponseEntity<>(body, status);
    }
    // ...
}
```

```yaml
server:
  error:
    path: /error
```

#### 4.2.3. 错误视图解析器

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
public class DefaultErrorViewResolver implements ErrorViewResolver, Ordered {
    private static final Map<Series, String> SERIES_VIEWS;
    static {
        Map<Series, String> views = new EnumMap<>(Series.class);
        views.put(Series.CLIENT_ERROR, "4xx");
        views.put(Series.SERVER_ERROR, "5xx");
        SERIES_VIEWS = Collections.unmodifiableMap(views);
    }
    // ...
    @Override
    public ModelAndView resolveErrorView(HttpServletRequest request,
                                         HttpStatus status,
                                         Map<String, Object> model) {
        // 根据HTTP状态码找到视图
        ModelAndView modelAndView = resolve(String.valueOf(status.value()), model);
        if (modelAndView == null && SERIES_VIEWS.containsKey(status.series())) {
            modelAndView = resolve(SERIES_VIEWS.get(status.series()), model);
        }
        return modelAndView;
    }
    private ModelAndView resolve(String viewName, Map<String, Object> model) {
        String errorViewName = "error/" + viewName;
        // ...
        return resolveResource(errorViewName, model);
    }
    // ...
}
```

#### 4.2.4. 白页的实现

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
public class ErrorMvcAutoConfiguration {
    // ...
    private static class StaticView implements View {
        // ...
        @Override
        public void render(Map<String, ?> model,
                           HttpServletRequest request,
                           HttpServletResponse response)
                throws Exception {
            // ...
            response.setContentType(TEXT_HTML_UTF8.toString());
            StringBuilder builder = new StringBuilder();
            Object timestamp = model.get("timestamp");
            Object message = model.get("message");
            Object trace = model.get("trace");
            if (response.getContentType() == null) {
                response.setContentType(getContentType());
            }
            builder.append("<html><body><h1>Whitelabel Error Page</h1>")
                    .append("<p>This application has no explicit mapping for /error, so you are seeing this as a fallback.</p>")
                    .append("<div id='created'>")
                    .append(timestamp)
                    .append("</div>")
                    .append("<div>There was an unexpected error (type=")
                    .append(htmlEscape(model.get("error")))
                    .append(", status=")
                    .append(htmlEscape(model.get("status")))
                    .append(").</div>");
            if (message != null) {
                builder.append("<div>").append(htmlEscape(message)).append("</div>");
            }
            if (trace != null) {
                builder.append("<div style='white-space:pre-wrap;'>")
                        .append(htmlEscape(trace))
                        .append("</div>");
            }
            builder.append("</body></html>");
            response.getWriter().append(builder.toString());
        }
        // ...
    }
    // ...
}
```

### 4.3. 错误处理步骤流程

```java
package org.springframework.web.servlet;
public class DispatcherServlet extends FrameworkServlet {
    protected void doDispatch(HttpServletRequest request,
                              HttpServletResponse response) throws Exception {
        // ...
        try {
            // ...
            try {
                // 执行控制器方法
            }
            catch (Exception ex) {
                dispatchException = ex;
            }
            catch (Throwable err) {
                dispatchException = new NestedServletException("Handler dispatch failed", err);
            }
            // 视图渲染，如果在执行控制器方法中发生异常，mv=null，异常信息会传入到视图渲染方法中
            processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);
        }
        catch (Exception ex) {
            triggerAfterCompletion(processedRequest, response, mappedHandler, ex);
        }
        catch (Throwable err) {
            triggerAfterCompletion(processedRequest, response, mappedHandler,
                    new NestedServletException("Handler processing failed", err));
        }
        finally {
            // ...
        }
    }
    private void processDispatchResult(HttpServletRequest request,
                                       HttpServletResponse response,
                                       @Nullable HandlerExecutionChain mappedHandler,
                                       @Nullable ModelAndView mv,
                                       @Nullable Exception exception) throws Exception {
        boolean errorView = false;
        if (exception != null) {
            if (exception instanceof ModelAndViewDefiningException) {
                // ...
            }
            else {
                Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);
                // 处理异常
                mv = processHandlerException(request, response, handler, exception);
                errorView = (mv != null);
            }
        }
        // 渲染视图
        // ...
    }
    @Nullable
    protected ModelAndView processHandlerException(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   @Nullable Object handler,
                                                   Exception ex) throws Exception {
        // ...
        ModelAndView exMv = null;
        if (this.handlerExceptionResolvers != null) {
            for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {
                exMv = resolver.resolveException(request, response, handler, ex);
                if (exMv != null) {
                    break;
                }
            }
        }
        if (exMv != null) {
            // ...
            return exMv;
        }
        throw ex;
    }
}
```

1. 执行目标方法
   1. 目标方法运行期间有任何异常都会被 catch
   2. 而且标志当前请求结束
   3. 并且用 `dispatchException` 保存执行目标方法时发生的异常
2. 进入视图解析流程（页面渲染）
   1. `processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);`
   2. 入参 `mv` 为 `null`，`dispatchException` 为异常信息
   3. `processDispatchResult` 方法
      1. 如果有异常信息，会处理异常（默认处理不了，会抛出异常）
      2. 如果没有异常信息，会正常渲染视图
3. 处理 handler 发生的异常，处理完成返回 `ModelAndView`
   1. `mv = processHandlerException(request, response, handler, exception);`
   2. 遍历所有的 `handlerExceptionResolvers`，看谁能处理当前异常
   3. `HandlerExceptionResolver` 处理器异常解析器
   4. `DefaultErrorAttributes` 先来处理异常。把异常信息保存到 request 域，并且返回 `null`
   5. 默认没有任何人能处理异常，所以异常会被抛出
4. 如果没有任何人能处理，最终底层就会发送 `/error` 请求，会被底层的 `BasicErrorController` 处理
   1. 解析错误视图，遍历所有的 `ErrorViewResolver` 看谁能解析
   2. 默认的 `DefaultErrorViewResolver`，作用是把响应状态码作为错误页的地址，`error/500.html`
   3. 模板引擎最终响应这个页面 `error/500.html`

#### 4.3.1. 异常解析器

![异常解析器](https://cdn.jsdelivr.net/gh/happyflyer/picture-bed@main/2021/异常解析器.2lmmvzpcius0.jpg)

```java
package org.springframework.web.servlet;
public interface HandlerExceptionResolver {
    @Nullable
    ModelAndView resolveException(HttpServletRequest request,
                                  HttpServletResponse response,
                                  @Nullable Object handler,
                                  Exception ex);
}
```

```java
package org.springframework.boot.web.servlet.error;
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DefaultErrorAttributes
        implements ErrorAttributes, HandlerExceptionResolver, Ordered {
    // ...
    private static final String ERROR_ATTRIBUTE = DefaultErrorAttributes.class.getName() + ".ERROR";
    // ...
    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        storeErrorAttributes(request, ex);
        return null;
    }
    private void storeErrorAttributes(HttpServletRequest request, Exception ex) {
        request.setAttribute(ERROR_ATTRIBUTE, ex);
    }
    // ...
}
```

```java
package org.springframework.web.servlet.handler;
public class HandlerExceptionResolverComposite
        implements HandlerExceptionResolver, Ordered {
    // ...
    @Override
    @Nullable
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         @Nullable Object handler,
                                         Exception ex) {
        if (this.resolvers != null) {
            for (HandlerExceptionResolver handlerExceptionResolver : this.resolvers) {
                ModelAndView mav = handlerExceptionResolver.resolveException(
                        request, response, handler, ex);
                if (mav != null) {
                    return mav;
                }
            }
        }
        return null;
    }
}
```

#### 4.3.2. 错误视图解析器补充

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
public class BasicErrorController extends AbstractErrorController {}
```

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
public abstract class AbstractErrorController implements ErrorController {
    // ...
    protected ModelAndView resolveErrorView(HttpServletRequest request,
                                            HttpServletResponse response,
                                            HttpStatus status,
                                            Map<String, Object> model) {
        for (ErrorViewResolver resolver : this.errorViewResolvers) {
            ModelAndView modelAndView = resolver.resolveErrorView(request, status, model);
            if (modelAndView != null) {
                return modelAndView;
            }
        }
        return null;
    }
}
```

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
@FunctionalInterface
public interface ErrorViewResolver {
    ModelAndView resolveErrorView(HttpServletRequest request,
                                  HttpStatus status,
                                  Map<String, Object> model);
}
```

```java
package org.springframework.boot.autoconfigure.web.servlet.error;
public class DefaultErrorViewResolver implements ErrorViewResolver, Ordered {}
```

#### 4.3.3. 异常解析器补充

- `HandlerExceptionResolver`
  - `HandlerExceptionResolverComposite`
  - `DefaultErrorAttributes`
  - `AbstractHandlerExceptionResolver`
    - `SimpleMappingExceptionResolver`
    - `AbstractHandlerMethodExceptionResolver`
      - `ExceptionHandlerExceptionResolver`
    - `DefaultHandlerExceptionResolver`
    - `ResponseStatusExceptionResolver`

```java
package org.springframework.web.servlet.handler;
public abstract class AbstractHandlerExceptionResolver
        implements HandlerExceptionResolver, Ordered {
    // ...
    @Override
    @Nullable
    public ModelAndView resolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable Object handler,
            Exception ex) {
        // ...
        ModelAndView result = doResolveException(request, response, handler, ex);
        // ...
    }
    // ...
    @Nullable
    protected abstract ModelAndView doResolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable Object handler,
            Exception ex);
    // ...
}
```

- `ExceptionHandlerExceptionResolver`

```java
package org.springframework.web.servlet.handler;
public abstract class AbstractHandlerMethodExceptionResolver
        extends AbstractHandlerExceptionResolver {
    // ...
    @Override
    @Nullable
    protected final ModelAndView doResolveException(
        HttpServletRequest request,
        HttpServletResponse response,
        @Nullable Object handler,
        Exception ex) {
        return doResolveHandlerMethodException(request, response, (HandlerMethod) handler, ex);
    }
    @Nullable
    protected abstract ModelAndView doResolveHandlerMethodException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable HandlerMethod handlerMethod,
            Exception ex);
}
```

```java
package org.springframework.web.servlet.mvc.method.annotation;
public class ExceptionHandlerExceptionResolver
        extends AbstractHandlerMethodExceptionResolver
        implements ApplicationContextAware, InitializingBean {
    // ...
    @Override
    @Nullable
    protected ModelAndView doResolveHandlerMethodException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable HandlerMethod handlerMethod,
            Exception exception) {
        // ...
    }
    // ...
}
```

- `ResponseStatusExceptionResolver`

```java
package org.springframework.web.servlet.mvc.annotation;
public class ResponseStatusExceptionResolver
        extends AbstractHandlerExceptionResolver
        implements MessageSourceAware {
    // ...
    @Override
    @Nullable
    protected ModelAndView doResolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable Object handler,
            Exception ex) {
        // ...
    }
    // ...
}
```

- `DefaultHandlerExceptionResolver`

```java
package org.springframework.web.servlet.mvc.support;
public class DefaultHandlerExceptionResolver
        extends AbstractHandlerExceptionResolver {
    // ...
    @Override
    @Nullable
    protected ModelAndView doResolveException(
            HttpServletRequest request,
            HttpServletResponse response,
            @Nullable Object handler,
            Exception ex) {
        // ...
    }
    // ...
}
```

### 4.4. 定制错误处理逻辑

- 自定义错误页
  - `error/404.html` 或 `error/5xx.html`
  - 有精确的错误状态码页面就匹配精确，没有就找 `4xx.html`
  - 如果都没有就触发白页
- `@ControllerAdvice` + `@ExceptionHandler` 处理全局异常
  - `@ExceptionHandler` 注解被 `ExceptionHandlerExceptionResolver` 解析
- `@ResponseStatus` + 自定义异常
  - `@ResponseStatus` 注解被 `ResponseStatusExceptionResolver` 解析
  - 把 `@ResponseStatus` 注解的信息底层调用 `response.sendError(statusCode, resolvedReason)`
  - tomcat 发送的 `/error`
- Spring 框架底层的异常，如 参数类型转换异常
  - 被 `DefaultHandlerExceptionResolver` 解析
  - 底层调用 `response.sendError(statusCode, ex.getMessage())`
  - 可以处理的 Spring 框架底层异常
    - `HttpRequestMethodNotSupportedException`, `405` (`SC_METHOD_NOT_ALLOWED`)
    - `HttpMediaTypeNotSupportedException`, `415` (`SC_UNSUPPORTED_MEDIA_TYPE`)
    - `HttpMediaTypeNotAcceptableException`, `406` (`SC_NOT_ACCEPTABLE`)
    - `MissingPathVariableException`, `500` (`SC_INTERNAL_SERVER_ERROR`)
    - `MissingServletRequestParameterException`, `400` (`SC_BAD_REQUEST`)
    - `ServletRequestBindingException`, `400` (`SC_BAD_REQUEST`)
    - `ConversionNotSupportedException`, `500` (`SC_INTERNAL_SERVER_ERROR`)
    - `TypeMismatchException`, `400` (`SC_BAD_REQUEST`)
    - `HttpMessageNotReadableException`, `400` (`SC_BAD_REQUEST`)
    - `HttpMessageNotWritableException`, `500` (`SC_INTERNAL_SERVER_ERROR`)
    - `MethodArgumentNotValidException`, `400` (`SC_BAD_REQUEST`)
    - `MissingServletRequestPartException`, `400` (`SC_BAD_REQUEST`)
    - `BindException`, `400` (`SC_BAD_REQUEST`)
    - `NoHandlerFoundException`, `404` (`SC_NOT_FOUND`)
    - `AsyncRequestTimeoutException`, `503` (`SC_SERVICE_UNAVAILABLE`)
- 自定义实现 `HandlerExceptionResolver` 处理异常
  - 可以调整优先级，可以作为全局异常处理规则
- `ErrorViewResolver` 实现自定义处理异常，一般不去自定义
  - `response.sendError()`，`/error` 请求就会转给 `basicErrorController`
  - 你的异常没有任何人能处理。tomcat 底层 `response.sendError()`，`/error` 请求就会转给 `basicErrorController`
  - `basicErrorController` 要去的页面地址是 `ErrorViewResolver`

#### 4.4.1. 自定义 2

```java
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler({ArithmeticException.class, NullPointerException.class})
    public String handleAlgorithmException(Exception e) {
        log.error("hhhh捕获到的异常是：{}", e);
        return "login";
    }
}
```

#### 4.4.2. 自定义 3

```java
@ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "用户数量太多")
public class UserTooManyException extends RuntimeException {
    public UserTooManyException() {
    }
    public UserTooManyException(String message) {
        super(message);
    }
}
```

```java
@Controller
public class TableController {
    @GetMapping("/dynamic_table")
    public String dynamicTable(Model model) {
        List<User> users = Arrays.asList(
                new User("user1", "pwd1"),
                new User("user2", "pwd2"),
                new User("user3", "pwd3"),
                new User("user4", "pwd4"),
                new User("user5", "pwd5"),
                new User("user6", "pwd6")
        );
        model.addAttribute("users", users);
        if (users.size() > 3) {
            throw new UserTooManyException();
        }
        return "table/dynamic_table";
    }
}
```

#### 4.4.3. 自定义 5

```java
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class CustomHandlerExceptionResolver implements HandlerExceptionResolver {
    @Override
    public ModelAndView resolveException(HttpServletRequest request,
                                         HttpServletResponse response,
                                         Object handler,
                                         Exception ex) {
        try {
            response.sendError(511, "我喜欢的错误");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ModelAndView();
    }
}
```

## 5. 原生 Servlet 组件注入

- Servlet
- Filter
- Listener

### 5.1. 使用 Servlet API

```java
@ServletComponentScan
@SpringBootApplication
public class SpringBoot2AdminApplication {
    public static void main(String[] args) {
        SpringApplication.run(SpringBoot2AdminApplication.class, args);
    }
}
```

```java
@WebServlet(urlPatterns = "/my")
public class MyServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.getWriter().write("666");
    }
}
```

```java
@Slf4j
@WebFilter(urlPatterns = {"/my", "/css/*"})
public class MyFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
        log.info("执行过滤器方法：{}.{}", "MyFilter", "init()");
    }
    @Override
    public void doFilter(ServletRequest request,
                         ServletResponse response,
                         FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request1 = (HttpServletRequest) request;
        log.info("|-->执行过滤器方法：{}.{} {} {}", "MyFilter", "doFilter()",
                "before", request1.getServletPath());
        chain.doFilter(request, response);
        log.info("-~->过滤的请求路径是: {}", request1.getServletPath());
        log.info("--|>执行过滤器方法：{}.{} {} {}", "MyFilter", "doFilter()",
                "after", request1.getServletPath());
    }
    @Override
    public void destroy() {
        Filter.super.destroy();
        log.info("执行过滤器方法：{}.{}", "MyFilter", "destroy()");
    }
}
```

```java
@Slf4j
@WebListener
public class MyListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContextListener.super.contextInitialized(sce);
        log.info("执行监听器方法：{}.{}", "MyListener", "contextInitialized()");
    }
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContextListener.super.contextDestroyed(sce);
        log.info("执行监听器方法：{}.{}", "MyListener", "contextDestroyed()");
    }
}
```

### 5.2. 使用 RegistrationBean

```java
@Configuration(proxyBeanMethods = true)
public class MyConfig {
    @Bean
    public ServletRegistrationBean myServlet() {
        MyServlet myServlet = new MyServlet();
        return new ServletRegistrationBean(myServlet, "/my1", "/my2");
    }
    @Bean
    public FilterRegistrationBean myFilter() {
        MyFilter myFilter = new MyFilter();
        // return new FilterRegistrationBean(myFilter, myServlet());
        FilterRegistrationBean bean = new FilterRegistrationBean(myFilter);
        bean.setUrlPatterns(Arrays.asList("/my1", "/css/*"));
        return bean;
    }
    @Bean
    public ServletListenerRegistrationBean myListener() {
        MyListener myListener = new MyListener();
        return new ServletListenerRegistrationBean<>(myListener);
    }
}
```

### 5.3. DispatcherServlet 自动配置

- 容器中自动配置了 `DispatcherServlet`
  - 属性绑定到 `WebMvcProperties`
  - 对应的配置文件配置项是 `spring.mvc`
- 通过 `ServletRegistrationBean<DispatcherServlet>`
  - 把 `DispatcherServlet` 配置进来
  - 默认映射的是 `/` 路径

```java
package org.springframework.boot.autoconfigure.web.servlet;
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass(DispatcherServlet.class)
@AutoConfigureAfter(ServletWebServerFactoryAutoConfiguration.class)
public class DispatcherServletAutoConfiguration {
    // ...
    @Configuration(proxyBeanMethods = false)
    @Conditional(DefaultDispatcherServletCondition.class)
    @ConditionalOnClass(ServletRegistration.class)
    @EnableConfigurationProperties(WebMvcProperties.class)
    protected static class DispatcherServletConfiguration {
        @Bean(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        public DispatcherServlet dispatcherServlet(WebMvcProperties webMvcProperties) {
            DispatcherServlet dispatcherServlet = new DispatcherServlet();
            dispatcherServlet.setDispatchOptionsRequest(webMvcProperties.isDispatchOptionsRequest());
            dispatcherServlet.setDispatchTraceRequest(webMvcProperties.isDispatchTraceRequest());
            dispatcherServlet.setThrowExceptionIfNoHandlerFound(webMvcProperties.isThrowExceptionIfNoHandlerFound());
            dispatcherServlet.setPublishEvents(webMvcProperties.isPublishRequestHandledEvents());
            dispatcherServlet.setEnableLoggingRequestDetails(webMvcProperties.isLogRequestDetails());
            return dispatcherServlet;
        }
        // ...
    }
    @Configuration(proxyBeanMethods = false)
    @Conditional(DispatcherServletRegistrationCondition.class)
    @ConditionalOnClass(ServletRegistration.class)
    @EnableConfigurationProperties(WebMvcProperties.class)
    @Import(DispatcherServletConfiguration.class)
    protected static class DispatcherServletRegistrationConfiguration {
        @Bean(name = DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
        @ConditionalOnBean(value = DispatcherServlet.class, name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
        public DispatcherServletRegistrationBean dispatcherServletRegistration(
                DispatcherServlet dispatcherServlet,
                WebMvcProperties webMvcProperties,
                ObjectProvider<MultipartConfigElement> multipartConfig) {
            DispatcherServletRegistrationBean registration = new DispatcherServletRegistrationBean(
                    dispatcherServlet, webMvcProperties.getServlet().getPath());
            registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
            registration.setLoadOnStartup(webMvcProperties.getServlet().getLoadOnStartup());
            multipartConfig.ifAvailable(registration::setMultipartConfig);
            return registration;
        }
    }
    // ...
}
```

```java
package org.springframework.boot.autoconfigure.web.servlet;
public class DispatcherServletRegistrationBean
        extends ServletRegistrationBean<DispatcherServlet>
        implements DispatcherServletPath {
    private final String path;
    // ...
}
```

```yaml
spring:
  mvc:
    servlet:
      path: /mvc
```

- Tomcat-Servlet 多个 Servlet 都能处理到同一层路径，精确优选原则
- `ServletA`：`/my`
- `ServletB`：`/my/1`

## 6. 嵌入式 Web 容器

### 6.1. web 版的 ioc 容器

1. `ServletWebServerApplicationContext` 容器启动
2. 寻找 `ServletWebServerFactory`
3. 引导创建服务器

![ServletWebServerApplicationContext](https://cdn.jsdelivr.net/gh/happyflyer/picture-bed@main/2021/ServletWebServerApplicationContext.1rf3mq7r2200.jpg)

```java
package org.springframework.boot.web.servlet.context;
public class ServletWebServerApplicationContext extends GenericWebApplicationContext
        implements ConfigurableWebServerApplicationContext {
    // ...
    private volatile WebServer webServer;
    // ...
    private void createWebServer() {
        // ...
        ServletWebServerFactory factory = getWebServerFactory();
        this.webServer = factory.getWebServer(getSelfInitializer());
        // ...
    }
    // ...
}
```

```java
package org.springframework.boot.web.servlet.server;
@FunctionalInterface
public interface ServletWebServerFactory {
    WebServer getWebServer(ServletContextInitializer... initializers);
}
```

### 6.2. 嵌入式 Web 容器原理

- SpringBoot 应用启动发现当前是 Web 应用
  - web 应用会创建一个 web 版的 ioc 容器 `ServletWebServerApplicationContext`
  - `ServletWebServerApplicationContext` 启动的时候寻找 `ServletWebServerFactory`
  - Servlet 的 `WebServer` 工厂 创建 `WebServer`
  - 容器中有很多的 `WebServer` 工厂中的一个
    - `TomcatServletWebServerFactory`
    - `JettyServletWebServerFactory`
    - `UndertowServletWebServerFactory`
- 底层直接会有一个自动配置类 `ServletWebServerFactoryAutoConfiguration`
  - 导入了 `ServletWebServerFactoryConfiguration` 配置类
  - 配置类根据动态判断系统中到底导入了那个 Web 服务器的包
- 默认 `starter-web` 导入 `starter-tomcat`，容器中就有 `TomcatServletWebServerFactory`
  - `TomcatServletWebServerFactory` 创建出 Tomcat 服务器并启动
  - `TomcatWebServer` 的构造器拥有初始化方法 `initialize` --- `this.tomcat.start();`
- 内嵌服务器，就是手动启动服务器的步骤 变成 代码调用（前提是 tomcat 核心 jar 包存在）
  - 默认支持的 webServer
    - Jetty
    - Netty
    - Tomcat
    - Undertow

#### 6.2.1. Web 服务器工厂自动配置

```java
package org.springframework.boot.autoconfigure.web.servlet;
@Configuration(proxyBeanMethods = false)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnClass(ServletRequest.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@EnableConfigurationProperties(ServerProperties.class)
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,
        ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,
        ServletWebServerFactoryConfiguration.EmbeddedJetty.class,
        ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })
public class ServletWebServerFactoryAutoConfiguration {
    @Bean
    public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(
            ServerProperties serverProperties) {
        return new ServletWebServerFactoryCustomizer(serverProperties);
    }
    @Bean
    @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")
    public TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(
            ServerProperties serverProperties) {
        return new TomcatServletWebServerFactoryCustomizer(serverProperties);
    }
    // ...
}
```

#### 6.2.2. Web 服务器工厂配置类

```java
package org.springframework.boot.autoconfigure.web.servlet;
@Configuration(proxyBeanMethods = false)
class ServletWebServerFactoryConfiguration {
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })
    @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
    static class EmbeddedTomcat {
        @Bean
        TomcatServletWebServerFactory tomcatServletWebServerFactory(...) {
            TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
            // ...
            return factory;
        }
    }
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ Servlet.class, Server.class, Loader.class, WebAppContext.class })
    @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
    static class EmbeddedJetty {
        @Bean
        JettyServletWebServerFactory JettyServletWebServerFactory(...) {
            JettyServletWebServerFactory factory = new JettyServletWebServerFactory();
            // ...
            return factory;
        }
    }
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass({ Servlet.class, Undertow.class, SslClientAuthMode.class })
    @ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)
    static class EmbeddedUndertow {
        @Bean
        UndertowServletWebServerFactory undertowServletWebServerFactory(...) {
            UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
            // ...
            return factory;
        }
    }
}
```

### 6.3. 切换嵌入式 Web 容器

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
  <exclusions>
    <exclusion>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-tomcat</artifactId>
    </exclusion>
  </exclusions>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-undertow</artifactId>
</dependency>
```

### 6.4. 定制 Servlet 容器

- 修改配置文件 `server.xxx`（推荐）
- 直接自定义 `ConfigurableServletWebServerFactory`
- 实现 `WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>`
  - 把配置文件的值和 `ServletWebServerFactory` 进行绑定
  - `xxxxCustomizer`：定制化器，可以改变 `xxxx` 的默认规则

```java
@Component
public class CustomizationBean
        implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {
    @Override
    public void customize(ConfigurableServletWebServerFactory server) {
        server.setPort(9000);
    }
}
```

## 7. 定制化原理

- Web 开发
  - 自动配置原理
  - 重要组件的工作流程

### 7.1. 原理分析套路

- 场景 starter
- `xxxxAutoConfiguration`
- 导入 xxxx 组件 `@Bean`
- 绑定 `xxxProperties`
- 绑定配置文件项

### 7.2. 定制化的常见方式

- 编写自定义的配置类 `xxxConfiguration` + `@Bean` 替换、增加容器中默认组件
- 修改配置文件（推荐）
- `xxxxCustomizer`
- Web 应用 编写一个配置类（推荐）
  - 实现 `WebMvcConfigurer` 即可定制化 web 功能
  - `@Bean` 给容器中再扩展一些组件
- `@EnableWebMvc` + `WebMvcConfigurer`（慎用）
  - `@Bean` 可以全面接管 SpringMVC，所有规则全部自己重新配置
  - 实现定制和扩展功能

```java
@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    public WebMvcRegistrations webMvcRegistrations() {
        return new WebMvcRegistrations() {
            @Override
            public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
                return WebMvcRegistrations.super.getRequestMappingHandlerMapping();
            }
            @Override
            public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter() {
                return WebMvcRegistrations.super.getRequestMappingHandlerAdapter();
            }
            @Override
            public ExceptionHandlerExceptionResolver getExceptionHandlerExceptionResolver() {
                return WebMvcRegistrations.super.getExceptionHandlerExceptionResolver();
            }
        };
    }
}
```

```java
@EnableWebMvc
@Configuration
public class MyWebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
```

```java
package org.springframework.web.servlet.config.annotation;
public interface WebMvcConfigurer {
    default void configurePathMatch(PathMatchConfigurer configurer) {}
    default void configureContentNegotiation(ContentNegotiationConfigurer configurer) {}
    default void configureAsyncSupport(AsyncSupportConfigurer configurer) {}
    default void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {}
    default void addFormatters(FormatterRegistry registry) {}
    default void addInterceptors(InterceptorRegistry registry) {}
    default void addResourceHandlers(ResourceHandlerRegistry registry) {}
    default void addCorsMappings(CorsRegistry registry) {}
    default void addViewControllers(ViewControllerRegistry registry) {}
    default void configureViewResolvers(ViewResolverRegistry registry) {}
    default void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {}
    default void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {}
    default void configureMessageConverters(List<HttpMessageConverter<?>> converters) {}
    default void extendMessageConverters(List<HttpMessageConverter<?>> converters) {}
    default void configureHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {}
    default void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {}
    @Nullable
    default Validator getValidator() { return null; }
    @Nullable
    default MessageCodesResolver getMessageCodesResolver() { return null; }
}
```

### 7.3. 全面接管 SpringMVC 的原理

- `WebMvcAutoConfiguration` 默认的 SpringMVC 的自动配置功能类。静态资源、欢迎页.....
- 一旦使用 `@EnableWebMvc`，会 `@Import(DelegatingWebMvcConfiguration.class)`
- `DelegatingWebMvcConfiguration` 的作用，只保证 SpringMVC 最基本的使用
  - 把所有系统中的 `WebMvcConfigurer` 拿过来
  - 所有功能的定制都是这些 `WebMvcConfigurer` 合起来一起生效
  - 自动配置了一些非常底层的组件，`RequestMappingHandlerMapping`、这些组件依赖的组件都是从容器中获取
  - `public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport`
- `WebMvcAutoConfiguration` 里面的配置要能生效 必须
  - `@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)`
- `@EnableWebMvc` 导致了 WebMvcAutoConfiguration 没有生效。

```java
package org.springframework.web.servlet.config.annotation;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebMvcConfiguration.class)
public @interface EnableWebMvc {
}
```

```java
package org.springframework.web.servlet.config.annotation;
@Configuration(proxyBeanMethods = false)
public class DelegatingWebMvcConfiguration extends WebMvcConfigurationSupport {
    private final WebMvcConfigurerComposite configurers = new WebMvcConfigurerComposite();
    @Autowired(required = false)
    public void setConfigurers(List<WebMvcConfigurer> configurers) {
        if (!CollectionUtils.isEmpty(configurers)) {
            this.configurers.addWebMvcConfigurers(configurers);
        }
    }
    @Override
    protected void configurePathMatch(PathMatchConfigurer configurer) {
        this.configurers.configurePathMatch(configurer);
    }
    // ...
}
```

```java
package org.springframework.web.servlet.config.annotation;
class WebMvcConfigurerComposite implements WebMvcConfigurer {
    private final List<WebMvcConfigurer> delegates = new ArrayList<>();
    public void addWebMvcConfigurers(List<WebMvcConfigurer> configurers) {
        if (!CollectionUtils.isEmpty(configurers)) {
            this.delegates.addAll(configurers);
        }
    }
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        for (WebMvcConfigurer delegate : this.delegates) {
            delegate.configurePathMatch(configurer);
        }
    }
    // ...
}
```

```java
package org.springframework.boot.autoconfigure.web.servlet;
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
@ConditionalOnClass({ Servlet.class, DispatcherServlet.class, WebMvcConfigurer.class })
@ConditionalOnMissingBean(WebMvcConfigurationSupport.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE + 10)
@AutoConfigureAfter({
        DispatcherServletAutoConfiguration.class,
        TaskExecutionAutoConfiguration.class,
        ValidationAutoConfiguration.class })
public class WebMvcAutoConfiguration {
}
```
