package com.ns.membership.adapter.in.web;

import com.ns.membership.adapter.in.web.dto.FriendRequest;
import com.ns.membership.application.port.in.FindMembershipUseCase;
import com.ns.membership.application.port.in.ModifyMembershipUseCase;
import com.ns.membership.application.port.in.RegisterMembershipUseCase;
import com.ns.membership.application.port.in.UserDataRequestUseCase;
import com.ns.membership.application.port.in.command.FindMembershipCommand;
import com.ns.membership.application.port.in.command.ModifyMembershipCommand;
import com.ns.membership.application.port.in.command.UserDataRequestCommand;
import com.ns.membership.domain.Membership;
import com.ns.membership.domain.userData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/membership/friend")
public class FriendController {

    private final ModifyMembershipUseCase modifyMembershipUseCase;
    private final FindMembershipUseCase findMembershipUseCase;
    private final UserDataRequestUseCase userDataRequestUseCase;

    @GetMapping("/{membershipId}")
    ResponseEntity<List<userData>> GetFriendList(@PathVariable String membershipId){
        //Todo 친구 목록을 표시합니다.
        try {
            Membership membership = findMembership(membershipId);

            if (membership == null)
                throw new RuntimeException("error : "+"membership is null.");

            UserDataRequestCommand command = UserDataRequestCommand.builder().
                    membershipId(membershipId)
                    .targetIdList(membership.getFriends()).build();

            log.info(membership.getFriends().toString());

            List<userData> detail = userDataRequestUseCase.getUserData(command);
            return ResponseEntity.ok(detail);
        } catch (Exception e){
            throw new RuntimeException("error : "+e);
        }
    }


    @GetMapping("/wanted/{membershipId}")
    ResponseEntity<List<userData>> GetWantedFriendList(@PathVariable String membershipId){
        //Todo 친구신청 목록을 표시합니다.
        try {
            Membership membership = findMembership(membershipId);

            if (membership == null)
                throw new RuntimeException("error : "+"membership is null.");

            UserDataRequestCommand command = UserDataRequestCommand.builder().
                membershipId(membershipId)
                .targetIdList(membership.getWantedFriends()).build();

            log.info(membership.getWantedFriends().toString());

            List<userData> detail = userDataRequestUseCase.getUserData(command);
            return ResponseEntity.ok(detail);
        } catch (Exception e){
            throw new RuntimeException("error : "+e);
        }
    }

    @PostMapping("/wanted/add")
    ResponseEntity<Membership> PostSendWantFriend(@RequestBody FriendRequest request){
        //Todo 친구 신청합니다.
        try {
            String membershipId = request.getMembershipId().toString();
            String targetmembershipId = request.getTargetId().toString();

            Membership membership = findMembership(membershipId);
            Membership targetMembership = findMembership(targetmembershipId);

            if (membership == null)
                throw new RuntimeException("error : "+"membership is null.");

            if (targetMembership == null)
                throw new RuntimeException("error : "+"target membership is null.");

            if (membership.getWantedFriends().contains(request.getTargetId()) || targetMembership.getWantedFriends().contains(request.getMembershipId()))
                throw new RuntimeException("error : "+"membership is already friend or wantedFriend.");

            Set<Long> wantedFriendList = new HashSet<>(targetMembership.getWantedFriends());
            wantedFriendList.add(request.getMembershipId());

            ModifyMembershipCommand modifyCommand = ModifyMembershipCommand.builder()
                    .membershipId(targetMembership.getMembershipId())
                    .name(targetMembership.getName())
                    .address(targetMembership.getAddress())
                    .email(targetMembership.getEmail())
                    .isValid(targetMembership.isValid())
                    .wantedFriends(wantedFriendList)
                    .build();

            return ResponseEntity.ok(modifyMembershipUseCase.modifyMembership(modifyCommand));
        }catch (Exception e){
            throw new RuntimeException("error : "+e);
        }
    }

    @PostMapping("/add")
    ResponseEntity<Membership> PostSendFriendAgree(@RequestBody FriendRequest request){
        //Todo 친구 신청을 수락합니다.
        try {
            String membershipId = request.getMembershipId().toString();
            String targetmembershipId = request.getTargetId().toString();

            Membership membership = findMembership(membershipId);
            Membership targetMembership = findMembership(targetmembershipId);

            if (membership == null)
                throw new RuntimeException("error : "+"membership is null.");

            if (targetMembership == null)
                throw new RuntimeException("error : "+"target membership is null.");

            if (membership.getFriends().contains(request.getTargetId()) || targetMembership.getFriends().contains(request.getMembershipId()))
                throw new RuntimeException("error : "+"membership is already friend or wantedFriend.");

            if (!membership.getWantedFriends().contains(request.getTargetId()))
                throw new RuntimeException("error : "+"targetId is not registered Member's wantedFriends List.");

            Set<Long> FriendList = new HashSet<>(membership.getFriends());
            FriendList.add(request.getTargetId());

            Set<Long> targetFriendList = new HashSet<>(targetMembership.getFriends());
            targetFriendList.add(request.getMembershipId());

            ModifyMembershipCommand myModifyCommand = ModifyMembershipCommand.builder()
                    .membershipId(membership.getMembershipId())
                    .name(membership.getName())
                    .address(membership.getAddress())
                    .email(membership.getEmail())
                    .isValid(membership.isValid())
                    .friends(FriendList)
                    .build();

            ModifyMembershipCommand targetModifyCommand = ModifyMembershipCommand.builder()
                    .membershipId(targetMembership.getMembershipId())
                    .name(targetMembership.getName())
                    .address(targetMembership.getAddress())
                    .email(targetMembership.getEmail())
                    .isValid(targetMembership.isValid())
                    .friends(targetFriendList)
                    .build();

            modifyMembershipUseCase.modifyMembership(targetModifyCommand);

            return ResponseEntity.ok(modifyMembershipUseCase.modifyMembership(myModifyCommand));
        }catch (Exception e){
            throw new RuntimeException("error : "+e);
        }
    }

    @PostMapping("/delete")
    ResponseEntity<Membership> PostDeleteFriend(@RequestBody FriendRequest request){
        //Todo 친구를 삭제합니다.
        try {
            String membershipId = request.getMembershipId().toString();
            String targetmembershipId = request.getTargetId().toString();

            Membership membership = findMembership(membershipId);
            Membership targetMembership = findMembership(targetmembershipId);

            if (membership == null)
                throw new RuntimeException("error : "+"membership is null.");

            if (targetMembership == null)
                throw new RuntimeException("error : "+"target membership is null.");

            if (membership.getFriends().contains(request.getTargetId()) || targetMembership.getFriends().contains(request.getMembershipId()))
                throw new RuntimeException("error : "+"membership is already friend or wantedFriend.");

            Set<Long> FriendList = new HashSet<>(membership.getFriends());
            if(!FriendList.remove(request.getTargetId()))
                throw new RuntimeException("error : "+"membership's friend remove fail.");

            Set<Long> targetFriendList = new HashSet<>(targetMembership.getFriends());
            if(!targetFriendList.remove(request.getMembershipId()))
                throw new RuntimeException("error : "+"target membership is null.");

            ModifyMembershipCommand myModifyCommand = ModifyMembershipCommand.builder()
                    .membershipId(membership.getMembershipId())
                    .name(membership.getName())
                    .address(membership.getAddress())
                    .email(membership.getEmail())
                    .isValid(membership.isValid())
                    .friends(FriendList)
                    .build();

            ModifyMembershipCommand targetModifyCommand = ModifyMembershipCommand.builder()
                    .membershipId(targetMembership.getMembershipId())
                    .name(targetMembership.getName())
                    .address(targetMembership.getAddress())
                    .email(targetMembership.getEmail())
                    .isValid(targetMembership.isValid())
                    .friends(targetFriendList)
                    .build();

            modifyMembershipUseCase.modifyMembership(targetModifyCommand);

            return ResponseEntity.ok(modifyMembershipUseCase.modifyMembership(myModifyCommand));
        }catch (Exception e){
            throw new RuntimeException("error : "+e);
        }
    }

    private Membership findMembership(String membershipId) {
        FindMembershipCommand findCommand = FindMembershipCommand.builder()
                .membershipId(membershipId)
                .build();

        return findMembershipUseCase.findMembership(findCommand);
    }


}
