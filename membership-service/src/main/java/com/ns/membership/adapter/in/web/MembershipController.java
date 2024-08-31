package com.ns.membership.adapter.in.web;

import com.ns.membership.adapter.in.web.dto.ModifyMembershipRequest;
import com.ns.membership.adapter.in.web.dto.RegisterMembershipRequest;
import com.ns.membership.adapter.in.web.dto.userDataCommands;
import com.ns.membership.adapter.out.JwtTokenProvider;
import com.ns.membership.application.port.in.FindMembershipUseCase;
import com.ns.membership.application.port.in.ModifyMembershipUseCase;
import com.ns.membership.application.port.in.RegisterMembershipUseCase;
import com.ns.membership.application.port.in.UserDataRequestUseCase;
import com.ns.membership.application.port.in.command.FindMembershipCommand;
import com.ns.membership.application.port.in.command.ModifyMembershipCommand;
import com.ns.membership.application.port.in.command.RegisterMembershipCommand;
import com.ns.membership.application.port.in.command.UserDataRequestCommand;
import com.ns.membership.domain.Membership;
import com.ns.membership.domain.userData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path="/membership")
public class MembershipController {


    private final RegisterMembershipUseCase registerMembershipUseCase;
    private final ModifyMembershipUseCase modifyMembershipUseCase;
    private final FindMembershipUseCase findMembershipUseCase;
    private final UserDataRequestUseCase userDataRequestUseCase;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping (path = "/register")
    Membership registerMembership(@RequestBody RegisterMembershipRequest request){
        // request -> Command로 추상화
        // UseCase ~~(request x, command)


        RegisterMembershipCommand command = RegisterMembershipCommand.builder()
                .name(request.getName())
                .address(request.getAddress())
                .email(request.getEmail())
                .isValid(true)
                .build();

        return registerMembershipUseCase.registerMembership(command);
    }

    @GetMapping (path = "/register/temp")
    Membership registerMembershipTemp(){

        Random random = new Random();

        RegisterMembershipCommand command = RegisterMembershipCommand.builder()
                .name("name:" + random.nextInt(10000))
                .address("address:" + random.nextInt(10000))
                .email("email: " + random.nextInt(10000))
                .isValid(true)
                .build();

        log.info("dummy create : "+command);

        return registerMembershipUseCase.registerMembership(command);
    }

    @PostMapping(path="/modify/")
    ResponseEntity<Membership> modifyMembershipByMemberId(@RequestBody ModifyMembershipRequest request){
        String membershipId = jwtTokenProvider.getMembershipIdbyToken().toString();

        if(membershipId != request.getMembershipId().toString())
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        FindMembershipCommand findCmmand = FindMembershipCommand.builder()
                .membershipId(membershipId)
                .build();

        Membership membership = findMembershipUseCase.findMembership(findCmmand);

        if(membership==null) {
            return ResponseEntity.notFound().build();
        }

        ModifyMembershipCommand modifyCommand = ModifyMembershipCommand.builder()
                .membershipId(request.getMembershipId())
                .name(request.getName())
                .address(request.getAddress())
                .email(request.getEmail())
                .isValid(request.isValid())
                .build();

        return ResponseEntity.ok(modifyMembershipUseCase.modifyMembership(modifyCommand));
    }

    @GetMapping(path="/{membershipId}")
    ResponseEntity<Membership> findMembershipByMemberId(@PathVariable String membershipId){

        FindMembershipCommand command = FindMembershipCommand.builder()
                .membershipId(membershipId)
                .build();

        return ResponseEntity.ok(findMembershipUseCase.findMembership(command));
    }

    @GetMapping (path = "/data/{membershipId}")
    ResponseEntity<userDataCommands> getUserData(@PathVariable String membershipId){
        String memberId = jwtTokenProvider.getMembershipIdbyToken().toString();

        if(memberId != membershipId)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        FindMembershipCommand findCmmand = FindMembershipCommand.builder()
                .membershipId(memberId)
                .build();

        Membership membership = findMembershipUseCase.findMembership(findCmmand);

        if(membership==null) {
            return ResponseEntity.notFound().build();
        }

        Set<Long> targetIdSet = new HashSet<>(Collections.singletonList(Long.parseLong(membershipId)));

        UserDataRequestCommand command = UserDataRequestCommand.builder().
                membershipId(memberId) //요청을 보낸 사용자의 id
                .targetIdList(targetIdSet).build();
        List<userData> detail = userDataRequestUseCase.getUserData(command);

        return ResponseEntity.ok(
                userDataCommands.builder()
                        .userDataCommandList(detail)
                        .build());
    }

    @GetMapping("/ally/random/{membershipId}")
    ResponseEntity<userDataCommands> getAllyRandom(@PathVariable String membershipId){

        //String memberId = jwtTokenProvider.getMembershipIdbyToken().toString();

        FindMembershipCommand findCmmand = FindMembershipCommand.builder()
                .membershipId(membershipId)
                .build();

        Membership membership = findMembershipUseCase.findMembership(findCmmand);

        if(membership==null) {
            return ResponseEntity.notFound().build();
        }

        Set<Long> targetIdList = userDataRequestUseCase.getAllyRandom(membershipId);

        UserDataRequestCommand command = UserDataRequestCommand.builder().
                membershipId(membershipId)
                .targetIdList(targetIdList).build();

        log.info(targetIdList.toString());
        List<userData> detail = userDataRequestUseCase.getUserData(command);
        return ResponseEntity.ok(
                userDataCommands.builder()
                        .userDataCommandList(detail)
                        .build()
        );
    }
}
