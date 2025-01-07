# resistance

---

![resistance](https://github.com/user-attachments/assets/11bce376-1c21-4cb8-8f5f-0f59f4691945)


> 20세기 말. 한국 근대사를 재조명해 역사의식을 키우기 위해 텀블벅 후원 준비중인 기능성 게임

<br>

1인 개발로 텀블벅 후원을 통해 출시할 목적으로 진행했지만, 현재 연출과 도트그래픽의 어려움으로 잠정 중단한 상태
- 사용자 데이터 관리, 텀블벅 후원 상품 제공 등을 위해 Flask 웹서버를 구현하면서 시작
- 이후 모놀리식 구조에서 확장성의 한계를 느껴 **MSA 마이그레이션**을 시도

<br>
<br>

**프로젝트 후기**

처음으로 MSA 프로젝트를 진행하면서 Hexagonal Architecture를 도입해 외부 라이브러리 사용에 대한 유연한 대응을 고민했다.

Apache Kafka와 `java.util.concurrent`의 CountDownLatch를 이용해서 각 마이크로 서비스간의 통신을 구현했다.

그 외에도 Spring AOP를 이용한 서비스 로깅, 책임 분리 원칙(SRP)를 위한 커스텀 어노테이션, `javax.validation`을 이용한 Command 검증 등 새로운 기술들을 많이 시도해볼 수 있는 경험이 되었다.


<br>



### 기술스택

---

![Java](https://img.shields.io/badge/Java-007396)
![C Sharp](https://img.shields.io/badge/C%20Sharp-68217A?style=flat-square&logo=csharp&logoColor=white)
![Unity3D](https://img.shields.io/badge/Unity3d-000000?style=flat-square&logo=unity&logoColor=white)

![Spring MVC](https://img.shields.io/badge/Spring%20MVC-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring OAuth2](https://img.shields.io/badge/Spring%20OAuth2-6DB33F?style=flat-square&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=flat-square&logo=spring-security&logoColor=white)

![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=flat-square&logo=mysql&logoColor=white)
![QueryDSL](https://img.shields.io/badge/QueryDSL-232F3E?style=flat-square&logo=amazon-aws&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-231F28?style=flat-square&logo=apache-kafka&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=flat-square&logo=redis&logoColor=white)

![Kubernetes](https://img.shields.io/badge/Kubernetes-326CE5?style=flat-square&logo=kubernetes&logoColor=white)
![Helm](https://img.shields.io/badge/Helm-0F1689?style=flat-square&logo=helm&logoColor=white)
![ArgoCD](https://img.shields.io/badge/ArgoCD-0E3C8C?style=flat-square&logo=argocd&logoColor=white)
![Argo Rollout](https://img.shields.io/badge/Argo%20Rollout-0E3C8C?style=flat-square&logo=argo%20rollout&logoColor=white)


<br> 

### 구현된 기능

---

📌 MSA 환경에서 트랜잭션의 Rollback은 복잡하고 리스크가 큰 작업이라 **유연한 대응**이라는 목적에 주객전도 된다고 생각함

가능하면 정석적인 트랜잭션의 구현을 최대한 피하거나, 기교로 해결하는게 좋다고 생각해 설계 과정에 많은 고민을 함

서비스간의 연계가 이뤄지는 대부분의 기능을 **온전히 조회를 목적으로 하도록 하위 도메인별로 설계했기에**, 따로 트랜잭션의 Rollback에 대한 고려를 하지 않음

<br>

각 모듈별 통신은 비동기적으로 이루어지도록 하기 위해서 Kafka를 이용해서 구현

Kafka IPC의 오류나 특정 서비스의 장애 상황 등 으로 해당 기능 (Business-service의 친구 목록, 동료 목록 조회)이 작동하지 않아도 서비스에 지장가지 않도록 구성


<br>

- **Github Action을 이용한 CI/CD 파이프라인 구축**
  - AWS ECR 이미지 빌드
  - Helm Repository 업데이트
  - Argo 배포 자동화로 Kubernetes 환경에서 운영 (Canary Deployment)


- **Bounded Context 설계를 통한 마이크로 서비스 분해**
  - [기술 블로그](https://downfa11.tistory.com/58)
  - Logging을 위해 Spring AOP를 활용한 공통 모듈과 마이크로 서비스 추가


- **Kafka를 이용한 각 마이크로 서비스간의 통신 구현**
  1. CountDownLatchManager에서 countDownLatch, 받아온 데이터를 각각 관리
  2. 다른 마이크로 서비스로 데이터를 요청할때 countDownLatch 설정하고 await()
  3. Kafka를 통해 데이터를 받아오면 읽어서 비즈니스 로직 진행


<br>

- **로그인과 회원가입**
  - Google, 카카오, 네이버 OAuth2와 Spring Security를 이용한 로그인 기능


- **재화 시스템**
  - 게임중 얻는 아이템 중에서 재화 시스템을 도입해 서버에서 환전
  - 환전이 활발하게 이루어지는 재화는 환율이 낮아지고, 반대 경우는 높아지도록 로직을 구성


- **시스템 공지사항**
  - 공지사항은 관리자만 작성, 수정 가능
  - 사용자는 주기적으로 확인하면서 신규 공지사항 등록시 알람 표시


- **게임 데이터 관리**
  - 사용자의 게임 데이터와 진행한 시나리오를 관리
    - 로그인시 불러오기, 주기적인 저장
  - 게임 옵션은 클라이언트에서 관리


- **Redis Hash 자료구조를 이용한 환율 시스템**
  - 서비스 내에서 이용되는 여러 재화(마재은, 청은, 엔화, 원화 등)에 대한 환율 시스템
  - 해당 재화를 사용시 서버에서 관리하는 전체 환율이 변동
    - 사용량이 많은 재화의 환율은 감소, 반대의 경우 증가하는 간단한 공식


- **친구 시스템**
  1. 스테이지 참가 이전에 **함께 데려갈 동료 리스트**를 랜덤으로 서버에서 추출
  2. 스테이지 종료 이후 친구 추가 여부를 묻고, 친구 추가를 하면 **친구신청대기** 상태
  3. 상대방이 수락하면 상호간 친구로 등록되어서 **함께 데려갈 동료 리스트**에 우선적으로 표시
  4. 친구 삭제 버튼을 통해 삭제

