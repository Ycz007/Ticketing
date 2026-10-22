package com.ticketing.ticket.model;

import com.fasterxml.jackson.annotation.JsonFormat;

public record HandlerOption(@JsonFormat(shape = JsonFormat.Shape.STRING) Long id, String displayName, String role) { }
