package com.ns.membership.application.security;

import com.ns.membership.adapter.out.persistance.MembershipJpaEntity;
import com.ns.membership.application.port.out.FindMembershipPort;
import com.ns.membership.domain.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final FindMembershipPort findMembershipPort;

    @Override
    public UserDetails loadUserByUsername(String account) throws UsernameNotFoundException {
        MembershipJpaEntity userData = findMembershipPort.findMembershipByAccount(
                new Membership.MembershipAccount(account));

        if (userData != null) {
            return new CustomUserDetails(userData);
        }

        return null;
    }
}
