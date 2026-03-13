import 'dart:io';

import 'package:douyin_kit/douyin_kit.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Douyin Kit Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.black),
        useMaterial3: true,
      ),
      home: const DemoPage(),
    );
  }
}

class DemoPage extends StatefulWidget {
  const DemoPage({super.key});

  @override
  State<DemoPage> createState() => _DemoPageState();
}

class _DemoPageState extends State<DemoPage> {
  static const String _defaultClientKey = 'aw14u9hdzmysru71';

  final ScrollController _scrollController = ScrollController();
  final List<String> _logs = <String>[];
  final String _clientKey = _defaultClientKey;

  // Token 测试用输入
  final TextEditingController _clientSecretCtrl = TextEditingController();
  final TextEditingController _codeCtrl = TextEditingController();
  final TextEditingController _refreshTokenCtrl = TextEditingController();
  final TextEditingController _openIdCtrl = TextEditingController();
  final TextEditingController _accessTokenCtrl = TextEditingController();

  // 分享用输入
  final TextEditingController _imageUriCtrl = TextEditingController();
  final TextEditingController _videoUriCtrl = TextEditingController();
  final TextEditingController _hashTagsCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    Douyin.instance.registerApp(clientKey: _clientKey);
    Douyin.instance.respStream().listen(_onResp);
    _log('已注册 clientKey: $_clientKey');
  }

  @override
  void dispose() {
    _scrollController.dispose();
    _clientSecretCtrl.dispose();
    _codeCtrl.dispose();
    _refreshTokenCtrl.dispose();
    _openIdCtrl.dispose();
    _accessTokenCtrl.dispose();
    _imageUriCtrl.dispose();
    _videoUriCtrl.dispose();
    _hashTagsCtrl.dispose();
    super.dispose();
  }

  void _onResp(dynamic data) {
    setState(() => _log('回调: $data'));
  }

  void _log(String msg) {
    setState(() {
      _logs.insert(0, '${DateTime.now().toString().substring(11, 19)} $msg');
      if (_logs.length > 100) _logs.removeLast();
    });
  }

  Future<void> _run(String label, Future<dynamic> Function() fn) async {
    try {
      _log('$label 请求中...');
      final result = await fn();
      _log('$label 成功: $result');
    } catch (e, st) {
      _log('$label 失败: $e');
      debugPrintStack(stackTrace: st);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Douyin Kit 全功能测试'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: Column(
        children: <Widget>[
          Expanded(
            child: ListView(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              children: <Widget>[
                _section('分享到联系人', <Widget>[
                  _inputRow('图片 file URI（单张）', _imageUriCtrl),
                  _wrapBtn('分享到联系人', () async {
                    // 1. 使用 load() 获取 assets 中图片的字节数据
                    final ByteData byteData = await rootBundle.load(
                      'assets/1.png',
                    );
                    // 2. 获取系统临时目录
                    final Directory tempDir = Directory.systemTemp;
                    // 3. 构建临时文件路径
                    final String tempPath = '${tempDir.path}/assets_1.png';
                    // 4. 将字节数据写入临时文件
                    final File imageFile = File(tempPath);
                    await imageFile.writeAsBytes(byteData.buffer.asUint8List());
                    // 5. 创建文件 URI 并分享
                    final Uri imageUri = Uri.file(imageFile.path);
                    Douyin.instance.shareImageToContacts(
                      imageUri: imageUri,
                      state: 'share_img_contacts',
                    );
                  }),
                  _wrapBtn('shareHtmlToContacts（固定内容）', () {
                    Douyin.instance.shareHtmlToContacts(
                      title: 'Flutter 抖音开放平台 SDK',
                      url: Uri.parse('https://qpweb.bjbhd.xyz/'),
                      discription: '抖音开放平台提供了丰富的 SDK 功能，欢迎体验！',
                      thumbUrl: Uri.parse(
                        'https://pic.rmb.bdstatic.com/bjh/bb87285bead/241026/076392b2350468578255552fcaca2a7c.jpeg@h_1280',
                      ),
                      state: 'share_html_contacts',
                    );
                  }),
                ]),
                _section('分享到快手', <Widget>[
                  _inputRow('图片 file URI（单张）', _imageUriCtrl),
                  _wrapBtn('分享到快手', () async {
                    // 1. 使用 load() 获取 assets 中图片的字节数据
                    final ByteData byteData = await rootBundle.load(
                      'assets/1.mp4',
                    );
                    // 2. 获取系统临时目录
                    final Directory tempDir = Directory.systemTemp;
                    // 3. 构建临时文件路径
                    final String tempPath = '${tempDir.path}/assets_1.mp4';
                    // 4. 将字节数据写入临时文件
                    final File imageFile = File(tempPath);
                    await imageFile.writeAsBytes(byteData.buffer.asUint8List());
                    // 5. 创建文件 URI 并分享
                    final Uri imageUri = Uri.file(imageFile.path);
                    Douyin.instance.ksShareVideo(
                      videoUris: <Uri>[imageUri],
                      state: 'ks_share_img',
                    );
                  }),
                ]),
                _section('环境检测', <Widget>[
                  _wrapBtn(
                    '是否安装抖音',
                    () => _run(
                      'isInstalled',
                      () => Douyin.instance.isInstalled(),
                    ),
                  ),
                  _wrapBtn(
                    '是否支持授权',
                    () => _run(
                      'isSupportAuth',
                      () => Douyin.instance.isSupportAuth(),
                    ),
                  ),
                  _wrapBtn(
                    '是否支持分享',
                    () => _run(
                      'isSupportShare',
                      () => Douyin.instance.isSupportShare(),
                    ),
                  ),
                  _wrapBtn(
                    '是否支持分享到联系人',
                    () => _run(
                      'isSupportShareToContacts',
                      () => Douyin.instance.isSupportShareToContacts(),
                    ),
                  ),
                  _wrapBtn(
                    '是否支持打开录制',
                    () => _run(
                      'isSupportOpenRecord',
                      () => Douyin.instance.isSupportOpenRecord(),
                    ),
                  ),
                ]),
                _section('授权', <Widget>[
                  _wrapBtn(
                    '授权 user_info',
                    () => Douyin.instance.auth(scope: <String>['user_info']),
                  ),
                  _wrapBtn(
                    '授权 user_info + state',
                    () => Douyin.instance.auth(
                      scope: <String>['user_info'],
                      state: 'test_state',
                    ),
                  ),
                ]),
                _section('Token / 用户信息（需先授权拿到 code）', <Widget>[
                  _inputRow('clientSecret', _clientSecretCtrl),
                  _inputRow('code', _codeCtrl),
                  _wrapBtn(
                    'getAccessToken',
                    () => _run(
                      'getAccessToken',
                      () => Douyin.instance.getAccessToken(
                        clientKey: _clientKey,
                        clientSecret: _clientSecretCtrl.text.trim(),
                        code: _codeCtrl.text.trim(),
                      ),
                    ),
                  ),
                  const Divider(height: 24),
                  _inputRow('refreshToken', _refreshTokenCtrl),
                  _wrapBtn(
                    'refreshAccessToken',
                    () => _run(
                      'refreshAccessToken',
                      () => Douyin.instance.refreshAccessToken(
                        clientKey: _clientKey,
                        refreshToken: _refreshTokenCtrl.text.trim(),
                      ),
                    ),
                  ),
                  const Divider(height: 24),
                  _inputRow('openId', _openIdCtrl),
                  _inputRow('accessToken', _accessTokenCtrl),
                  _wrapBtn(
                    'getUserInfo',
                    () => _run(
                      'getUserInfo',
                      () => Douyin.instance.getUserInfo(
                        openId: _openIdCtrl.text.trim(),
                        accessToken: _accessTokenCtrl.text.trim(),
                      ),
                    ),
                  ),
                ]),
                _section('分享到抖音', <Widget>[
                  _inputRow('图片 file URI（多个用逗号分隔）', _imageUriCtrl),
                  _wrapBtn('shareImage', () {
                    final List<Uri> uris = _imageUriCtrl.text
                        .split(',')
                        .map((String e) => Uri.parse(e.trim()))
                        .where((Uri u) => u.toString().isNotEmpty)
                        .toList();
                    if (uris.isEmpty) {
                      _log('shareImage: 请填写 file:// 图片路径');
                      return;
                    }
                    Douyin.instance.shareImage(
                      imageUris: uris,
                      state: 'share_image',
                    );
                  }),
                  _wrapBtn('shareImage（使用 assets/1.png）', () {
                    Douyin.instance.shareImage(
                      imageUris: <Uri>[
                        Uri.parse(
                          'file:///android_asset/flutter_assets/packages/douyin_kit/assets/1.png',
                        ),
                      ],
                      state: 'share_image_assets',
                    );
                  }),
                  _inputRow('视频 file URI（多个用逗号分隔）', _videoUriCtrl),
                  _wrapBtn('shareVideo', () {
                    final List<Uri> uris = _videoUriCtrl.text
                        .split(',')
                        .map((String e) => Uri.parse(e.trim()))
                        .where((Uri u) => u.toString().isNotEmpty)
                        .toList();
                    if (uris.isEmpty) {
                      _log('shareVideo: 请填写 file:// 视频路径');
                      return;
                    }
                    Douyin.instance.shareVideo(
                      videoUris: uris,
                      state: 'share_video',
                    );
                  }),
                  _wrapBtn('shareVideo（使用 assets/1.mp4）', () {
                    Douyin.instance.shareVideo(
                      videoUris: <Uri>[
                        Uri.parse(
                          'file:///android_asset/flutter_assets/packages/douyin_kit/assets/1.mp4',
                        ),
                      ],
                      state: 'share_video_assets',
                    );
                  }),
                  _wrapBtn('ksShareVideo（快手）', () {
                    final List<Uri> uris = _videoUriCtrl.text
                        .split(',')
                        .map((String e) => Uri.parse(e.trim()))
                        .where((Uri u) => u.toString().isNotEmpty)
                        .toList();
                    if (uris.isEmpty) {
                      _log('ksShareVideo: 请填写 file:// 视频路径');
                      return;
                    }
                    Douyin.instance.ksShareVideo(
                      videoUris: uris,
                      state: 'ks_share',
                    );
                  }),
                  _inputRow('话题（多个用逗号分隔）', _hashTagsCtrl),
                  _wrapBtn('shareHashTags', () {
                    final List<String> tags = _hashTagsCtrl.text
                        .split(',')
                        .map((String e) => e.trim())
                        .where((String e) => e.isNotEmpty)
                        .toList();
                    if (tags.isEmpty) {
                      _log('shareHashTags: 请填写话题');
                      return;
                    }
                    Douyin.instance.shareHashTags(
                      hashTags: tags,
                      state: 'share_hashtags',
                    );
                  }),
                ]),

                _section('录制', <Widget>[
                  _wrapBtn(
                    'openRecord',
                    () => Douyin.instance.openRecord(state: 'open_record'),
                  ),
                ]),
              ],
            ),
          ),
          _logPanel(),
        ],
      ),
    );
  }

  Widget _section(String title, List<Widget> children) {
    return Card(
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Text(title, style: Theme.of(context).textTheme.titleMedium),
            const SizedBox(height: 8),
            ...children,
          ],
        ),
      ),
    );
  }

  Widget _wrapBtn(String label, VoidCallback onPressed) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: ElevatedButton(onPressed: onPressed, child: Text(label)),
    );
  }

  Widget _inputRow(String label, TextEditingController ctrl) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 8),
      child: TextField(
        controller: ctrl,
        decoration: InputDecoration(
          labelText: label,
          border: const OutlineInputBorder(),
          isDense: true,
        ),
      ),
    );
  }

  Widget _logPanel() {
    return Container(
      height: 160,
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: Colors.grey.shade200,
        border: Border(top: BorderSide(color: Colors.grey.shade400)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: <Widget>[
              Text('日志', style: Theme.of(context).textTheme.titleSmall),
              TextButton(
                onPressed: () => setState(() => _logs.clear()),
                child: const Text('清空'),
              ),
            ],
          ),
          Expanded(
            child: ListView.builder(
              itemCount: _logs.length,
              itemBuilder: (_, int i) => SelectableText(
                _logs[i],
                style: const TextStyle(fontSize: 11, fontFamily: 'monospace'),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
