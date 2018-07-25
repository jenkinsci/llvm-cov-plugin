# llvm-cov-plugin

This plugin allows you to capture JSON format code coverage reports from [llvm-cov](https://llvm.org/docs/CommandGuide/llvm-cov.html). It implements code-coverage-api-plugin and can generate coverage chart, coverage trend chart and source code with coverage navigation. 

## How to use it

- Install the llvm-cov plugin
- Configure your project's build script to generate JSON format coverage report from llvm-cov
- Enable "Publish Coverage Report" publisher in the Post-build Actions
- Add llvm-cov in "Publish Coverage Report" publisher and specify reports path.
- (Optional) Specify Thresholds of each metrics
- (Optional) Specify Source code storing level to enable source code navigation