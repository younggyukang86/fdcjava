# Rabbit Mq Listener

- 해당 소스는 Rabbit Mq 수신 테스트 소스로서 Rabbit Mq 송신 테스트 소스와 같이 동작해야 됨.
- 수신, 송신 테스트를 위해서는 Rabbit Mq 서비스 설치가 필요함.

## Rabbit Mq 설치 방법

1. Erlang 설치

2. rabbit mq 설치

3. 레빗mq 프롬포트 실행후 rabbitmq-plugins enable rabbitmq_management 입력

4. rabbitmq-service.bat stop
   rabbitmq-service.bat install
   rabbitmq-service.bat start

5. http://localhost:15672 접속
   guest / guest

6. Admin탭에서 rabbitmq 사용자 추가

7. Exchange탭에서 bbs.exchange 추가

8. Queues탭에서 bbs.queue.java 추가

## Native Build 방법

의존성 모듈중에 SLF4J Simple(로그)로 인하여 빌드전 Agent등록이 필요함.

1. Agent 등록 (참고로 agent 등록시 등록한 의존성 모듈들을 동작해줘야 등록됨. 그래서 Main 함수에서 해당 모듈 소스들을 작성하여 Main 실행시 동작하도록 해줘야 함)
   -> java -agentlib:native-image-agent=config-output-dir=C:\Project\graalvmTest\rmqListener\build\libs\agent -jar rmqListener.jar

2. Native 빌드 (ConfigurationFileDirectories 옵션에 등록한 agent 경로 입력)
   -> native-image -H:-CheckToolchain -jar rmqListener.jar -H:ConfigurationFileDirectories=C:\Project\graalvmTest\rmqListener\build\libs\agent