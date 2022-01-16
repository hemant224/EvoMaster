package com.foo.micronaut.rest;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.validation.constraints.PositiveOrZero;

@Controller()
public class IndexController {

    @Operation(summary = "Index controller to crash micronaut with 500",
            description = "To test the crash scenario."
    )
    @ApiResponse(responseCode = "500", description = "Expected outcome")
    @Get(produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> index() {
        // it is expected the application to send connection close when it crashes
        throw new ExperimentalException();
    }

    @Operation(summary = "POST Controller for test",
            description = "Return 200"
    )
    @ApiResponse(responseCode = "200", description = "Working POST route")
    @Post(value="{?x,y}", produces = MediaType.APPLICATION_JSON)
    public HttpResponse<String> indexPost(@Nullable @PositiveOrZero Integer x, @Nullable @PositiveOrZero Integer y) {
        int z = ( x != null && y != null) ? x + y : 0;
        return HttpResponse.status(HttpStatus.OK).body("{\"message\":\"Working!\",\"answer\":" + z + "}");
    }

}