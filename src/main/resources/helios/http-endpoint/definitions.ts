interface HttpEndpointContext<Config = any> {
    request: Java.HttpServletRequest;
    response: Java.HttpServletResponse;
    adminConfig: Config;
}

interface HttpEndpointFunction<Config = any> {
    (context: HttpEndpointContext<Config>, runtime: Hyperion.Runtime): any;
}

interface GetSampleAdminConfigFunction {
    (): string;
}