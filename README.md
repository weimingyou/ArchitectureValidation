# ArchitectureValidation
To validate a Java project's architecture is not broken at compiling time.

To use it in your project, please check this sample project https://github.com/weimingyou/TestArchitectureValidator

In your Java project, add a Java class or you can use an existing class (not in default package), 
add annotation like @ArchitectureValidation(disallow={"org:com"}). Here "org:com" means package "org" should not use anything from "com" package.