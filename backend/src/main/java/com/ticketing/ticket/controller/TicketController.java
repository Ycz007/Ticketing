package com.ticketing.ticket.controller;

import com.ticketing.auth.security.SessionUser;
import com.ticketing.ticket.dto.TicketRequests.*;
import com.ticketing.ticket.model.*;
import com.ticketing.ticket.service.TicketService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TicketController {
    private final TicketService service;
    public TicketController(TicketService service) { this.service=service; }
    @PostMapping("/tickets") public TicketRow create(@AuthenticationPrincipal SessionUser user,@Valid @RequestBody Create body) { return service.create(user,body); }
    @GetMapping("/tickets") public TicketService.Page list(@AuthenticationPrincipal SessionUser user,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(required=false) String status,@RequestParam(required=false) String category,
            @RequestParam(required=false) String requestId,@RequestParam(defaultValue="false") boolean unassigned) {
        return service.list(user,page,size,status,category,requestId,unassigned);
    }
    @GetMapping("/tickets/{id}") public TicketService.Detail detail(@AuthenticationPrincipal SessionUser user,@PathVariable Long id) { return service.detail(user,id); }
    @GetMapping("/permissions") public List<PermissionOption> permissions(@AuthenticationPrincipal SessionUser user) { return service.permissions(user); }
    @GetMapping("/handlers") public List<HandlerOption> handlers(@AuthenticationPrincipal SessionUser user) { return service.handlers(user); }
    @GetMapping("/workbench/counts") public Map<String,Long> counts(@AuthenticationPrincipal SessionUser user) { return service.counts(user); }
    @PostMapping("/tickets/{id}/assign") public TicketRow assign(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Assign r) { return service.assign(u,id,r); }
    @PostMapping("/tickets/{id}/category") public TicketRow category(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Category r) { return service.category(u,id,r); }
    @PostMapping("/tickets/{id}/start") public TicketRow start(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Version r) { return service.start(u,id,r); }
    @PostMapping("/tickets/{id}/comments") public TicketRow comment(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Comment r) { return service.comment(u,id,r); }
    @PostMapping("/tickets/{id}/result") public TicketRow result(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Result r) { return service.result(u,id,r); }
    @PostMapping("/tickets/{id}/approve") public TicketRow approve(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Result r) { return service.approve(u,id,r); }
    @PostMapping("/tickets/{id}/confirm") public TicketRow confirm(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Version r) { return service.confirm(u,id,r); }
    @PostMapping("/tickets/{id}/rework") public TicketRow rework(@AuthenticationPrincipal SessionUser u,@PathVariable Long id,@Valid @RequestBody Rework r) { return service.rework(u,id,r); }
}
