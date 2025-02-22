//package com.ns.membership.application.security;
//
//import com.ns.membership.adapter.in.web.dto.oauth2.GoogleUserInfo;
//import com.ns.membership.adapter.in.web.dto.oauth2.KakaoUserInfo;
//import com.ns.membership.adapter.in.web.dto.oauth2.NaverUserInfo;
//import com.ns.membership.adapter.in.web.dto.oauth2.OAuth2UserInfo;
//import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
//import com.ns.membership.application.port.out.FindMembershipPort;
//import com.ns.membership.application.port.out.RegisterMembershipPort;
//import com.ns.membership.domain.Membership;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
//import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
//import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.stereotype.Service;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class Oauth2UserDetailsService extends DefaultOAuth2UserService {
//
//    @Autowired
//    private BCryptPasswordEncoder bCryptPasswordEncoder;
//
//    @Autowired
//    private final FindMembershipPort findMembershipPort;
//
//    @Autowired
//    private final RegisterMembershipPort registerMembershipPort;
//
//    // 구글로부터 받은 userRequest 데이터에 대한 후처리되는 함수
//    // 함수 종료시 @AuthenticationPrincipal 어노테이션이 만들어진다.
//    @Override
//    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
//        log.info("getClientRegistration = " + userRequest.getClientRegistration()); // registrationId로 어떤 OAuth로 로그인 했는지 확인 가능
//        log.info("getAccessToken = " + userRequest.getAccessToken().getTokenValue());
//
//        OAuth2User oAuth2User = super.loadUser(userRequest);
//        // 구글로그인 버튼 클릭 -> 구글로그인창 -> 로그인을 완료 -> code를 리턴(OAuth2-Client 라이브러리) -> AccessToken 요청
//        // userRequest 정보 -> 회원 프로필 받아야함(loadUser함수 호출) -> 구글로부터 회원프로필 받아준다.
//        log.info("getAttributes = " + oAuth2User.getAttributes());
//
//        OAuth2UserInfo oAuth2UserInfo = null;
//        if (userRequest.getClientRegistration().getRegistrationId().equals("google")) {
//            log.info("구글 로그인 요청");
//            oAuth2UserInfo = new GoogleUserInfo(oAuth2User.getAttributes());
//        } else if (userRequest.getClientRegistration().getRegistrationId().equals("naver")) {
//            log.info("네이버 로그인 요청");
//            oAuth2UserInfo = new NaverUserInfo(oAuth2User.getAttributes());
//        } else if (userRequest.getClientRegistration().getRegistrationId().equals("kakao")) {
//            log.info("카카오 로그인 요청");
//            oAuth2UserInfo = new KakaoUserInfo(oAuth2User.getAttributes());
//        }
//        else {
//            log.info("지원하지 않는 OAuth2UserInfo 입니다.");
//        }
//
//        String provider = oAuth2UserInfo.getProvider(); // google
//        String providerId = oAuth2UserInfo.getProviderId();
//        String account = provider + "_" + providerId; // google_10021320120
//        String password = bCryptPasswordEncoder.encode("test");
//        String email = oAuth2UserInfo.getEmail();
//        String role = "ROLE_USER";
//
//        MembershipJpaEntity userData = findMembershipPort.findMembershipByAccount(
//                new Membership.MembershipAccount(account)).get();
//
//        if (userData == null) {
//            log.info("로그인이 최초입니다.");
//            registerMembershipPort.createMembership(
//                    new Membership.MembershipName(account), // todo.
//                    new Membership.MembershipAccount(account),
//                    new Membership.MembershipPassword(password),
//                    new Membership.MembershipAddress("임시 주소"), // todo.
//                    new Membership.MembershipEmail(email),
//                    new Membership.MembershipIsValid(true),
//                    new Membership.Friends(null),
//                    new Membership.WantedFriends(null),
//                    new Membership.RefreshToken(""),
//                    new Membership.MembershipRole(role),
//                    new Membership.MembershipProvider(provider),
//                    new Membership.MembershipProviderId(providerId)
//            );
//        } else {
//            log.info("구글 로그인을 이미 한적이 있습니다. 당신은 자동회원가입이 되어 있습니다.");
//        }
//
//        // 회원 가입을 강제로 진행해볼 예정
//        return new CustomUserDetails(userData, oAuth2User.getAttributes());
//    }
//}
