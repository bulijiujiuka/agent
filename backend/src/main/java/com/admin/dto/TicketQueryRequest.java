package com.admin.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TicketQueryRequest extends PageRequest {

    private String title;

    private String category;

    private String priority;

    private String ticketStatus;
}
