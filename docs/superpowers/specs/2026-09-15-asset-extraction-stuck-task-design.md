# 资产提取任务卡住修复设计

## 目标

解决点击“提取人物/场景”后任务永久停留在 `PROCESSING`、前端无限轮询的问题。

## 方案

资产提取任务执行从领取任务开始就纳入统一异常处理。任何领取后的异常都将任务更新为 `FAILED`，避免数据库留下无法结束的状态。

对于数据库中长时间没有更新的 `PROCESSING` 任务，在用户再次发起同一章节提取时，将其恢复为 `PENDING` 并重新提交后台执行。默认超时时间为 10 分钟，避免误抢占正常运行中的任务。

## 数据流

`PENDING -> PROCESSING -> COMPLETED/PARTIAL_FAILED/FAILED`

孤儿任务：`PROCESSING --超过超时阈值--> PENDING -> PROCESSING`

## 测试

- 首次进度更新失败时，任务被标记为 `FAILED`。
- 超时的 `PROCESSING` 任务可以被恢复并重新提交。
- 未超时的 `PROCESSING` 任务保持原任务，不重复提交。
