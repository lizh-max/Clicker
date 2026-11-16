# 连点器（Clicker）

离线、零 root、最小权限的安卓手机连点器。

## 构建产物

| 文件 | 说明 |
|---|---|
| `app/build/outputs/apk/release/app-release.apk` | **签名版，可直接安装** |
| `app/build/outputs/apk/debug/app-debug.apk` | 调试版（默认 debug 签名） |

## 权限（最小化）

仅 1 项危险权限：`SYSTEM_ALERT_WINDOW`（悬浮窗）。无 INTERNET、无存储、无定位。

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

1. 安装 APK 后打开，先开启「无障碍服务」和「悬浮窗权限」
2. 设置点击坐标、间隔、拟人化参数
3. 保存配置，点击「显示悬浮球」
4. 切到目标页面，点悬浮球展开面板，点「启动」

## 功能状态（M1）

- [x] 无障碍服务单点连点
- [x] 悬浮球 + 控制面板（启动/暂停/停止）
- [x] 权限引导
- [x] 拟人化参数（坐标偏移 / 间隔抖动 / 按压时长）
- [ ] 多步序列 + 滑动（M2）
- [ ] 取点辅助（M3）
