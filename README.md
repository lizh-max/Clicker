# 连点器（Clicker）

离线、零 root、最小权限的安卓手机连点器。

## 构建产物

| 文件 | 说明 |
|---|---|
| `app/build/outputs/apk/release/app-release.apk` | **签名版，可直接安装** |
| `app/build/outputs/apk/debug/app-debug.apk` | 调试版（默认 debug 签名） |

## 权限

| 权限 | 类型 | 用途 |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | 危险权限（需引导授权） | 悬浮球 / 点位标记 / 编辑面板 |
| `FOREGROUND_SERVICE` | 普通 | 前台服务，防止悬浮球切后台后被系统回收 |
| `FOREGROUND_SERVICE_SPECIAL_USE` | 普通 | Android 14 要求显式声明 specialUse 类型 |
| `POST_NOTIFICATIONS` | 运行时（Android 13+） | 显示「连点器运行中」常驻通知 |

**无 INTERNET、无存储、无定位**（离线可用）。

> 说明：为保证悬浮球在切到目标 App 后不被系统回收，悬浮窗服务以前台服务方式运行，通知栏会常驻一条「连点器运行中」通知（Android 8.0+ 前台服务的硬性要求）。关闭悬浮窗后通知消失。

## 本地构建

```bash
export JAVA_HOME="/Users/hui/Library/Java/JavaVirtualMachines/ms-17.0.18/Contents/Home"
export ANDROID_HOME="/Users/hui/AndroidSdk"
./.tools/gradle-8.5/bin/gradle --no-daemon assembleRelease
```

## 签名信息（开发用）

- keystore: `keystore/clicker.keystore`
- 别名: `clicker`，密码: `clicker123`（仅供本地开发，正式发布请更换）

## 使用说明

1. 安装 APK 后打开，开启「无障碍服务」和「悬浮窗权限」（Android 13+ 建议同时允许通知）
2. 点「显示悬浮球」，屏幕出现悬浮球
3. 点悬浮球 →「取点」，屏幕中央生成一个点位，拖动到目标位置（可多次取点，多点位按序号执行）
4. 点某个点位标记，可单独设置该点位的类型、时长、间隔、抖动、坐标偏移、按压时长等参数
5. 点「启动」开始连点；运行中面板只保留「暂停/继续」和「停止」

## 功能状态

- [x] 无障碍服务单点/多点连点 + 滑动手势
- [x] 悬浮球 + 控制面板（启动 / 暂停 / 继续 / 停止 / 关闭）
- [x] 权限引导（无障碍 + 悬浮窗检测跳转）
- [x] 前台服务保活（防止切后台后悬浮球被系统回收）
- [x] 点位数字标记（拖拽移动、点击编辑、删除）
- [x] 取点辅助（屏幕中央生成点位，可拖动）
- [x] 每点位独立拟人化参数（间隔 / 抖动 / 坐标偏移 / 按压时长，非零默认值）
- [x] 配置本地持久化（SharedPreferences，无网络）
- [ ] 任务列表管理（多任务保存切换）
- [ ] 滑动轨迹曲线（贝塞尔缓动）
- [ ] 屏幕变化自动暂停（安全层）
