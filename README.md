spring framework 5.3.x 기준으로 작성하였습니다

## spring-webmvc 모듈과 coroutine 통합과정

[actual implementation](https://github.com/spring-projects/spring-framework/blob/3adabf391f91a7505ea29226d28479221739a8ba/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ServletInvocableHandlerMethod.java#L107-L144)

invokeForRequest 함수를 호출하여 request handler(like controller method)를 실행한다.  
다만, request handler가 kotlin의 suspend function일 경우에 spring은 request handler를 바로 실행하지 않고 reactive streams 시퀀스를 반환하는 returnType으로 변환한다  
> 실제로 개발자가 구현한 함수는 [ResponseBodyEmitterReturnValueHandler](https://github.com/spring-projects/spring-framework/blob/5.3.x/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitterReturnValueHandler.java)에 의해 실행 됨

### 관련 구현체들

[비동기 동작 제어논리 in DispatcherServlet](https://github.com/spring-projects/spring-framework/blob/3adabf391f91a7505ea29226d28479221739a8ba/spring-webmvc/src/main/java/org/springframework/web/servlet/DispatcherServlet.java#L1032-L1112)

suspend function을 감지하고 returnType을 wrapping하는 구현 (아래 두 클래스의 연쇄 작용으로 처리)
- [KotlinDetector.isSuspendingFunction](https://github.com/spring-projects/spring-framework/blob/3adabf391f91a7505ea29226d28479221739a8ba/spring-core/src/main/java/org/springframework/core/KotlinDetector.java#L82-L90)  
  suspend 함수 감지
- [CoroutinesUtils.invokeSuspendingFunction](https://github.com/spring-projects/spring-framework/blob/3adabf391f91a7505ea29226d28479221739a8ba/spring-core/src/main/java/org/springframework/core/CoroutinesUtils.java#L68-L86)  
  suspend function을 reactive streams로 변경 (전용 스레드 디스패쳐 없음)
  > spring-web project에서 request handler 함수를 suspend로 정의할 경우 runtime에 reactor 의존성을 요구하는 이유
  
비동기 동작과 관련된 `HandlerMethodReturnValueHandler` 구현체 목록의 일부 ([컴포지션 클래스](https://github.com/spring-projects/spring-framework/blob/5.3.x/spring-messaging/src/main/java/org/springframework/messaging/handler/invocation/HandlerMethodReturnValueHandlerComposite.java)에 의해 적절히 선택됨)
- [CallableMethodReturnValueHandler](https://github.com/spring-projects/spring-framework/blob/5.3.x/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/CallableMethodReturnValueHandler.java)
- [DeferredResultMethodReturnValueHandler](https://github.com/spring-projects/spring-framework/blob/5.3.x/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/DeferredResultMethodReturnValueHandler.java)
- [ResponseBodyEmitterReturnValueHandler](https://github.com/spring-projects/spring-framework/blob/5.3.x/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/ResponseBodyEmitterReturnValueHandler.java) 

AsyncManager 상태 변경
- HandlerMethodReturnValueHandler의 구현체중 지연 연산을 지원하는 구현체들은 `WebAsyncManager.startXXXProcessing` 함수를 호출하여 request 처리 흐름제어에 기여함
