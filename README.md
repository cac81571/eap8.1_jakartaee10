# EAP 8.1 で Jakarta EE 10 を動かす Docker プロジェクト

このプロジェクトは、Jakarta EE 10 のサンプル WAR をビルドし、JBoss EAP 8.1 コンテナにデプロイして実行する最小構成です。
以下の Red Hat ブログ記事の流れに合わせて、EAP builder/runtime のマルチステージで構成しています。
https://rheb.hatenablog.com/entry/2024/06/28/162306

## 前提

- Docker / Docker Compose が利用可能
- Red Hat コンテナレジストリへログイン可能
  - `registry.redhat.io/jboss-eap-8/eap81-openjdk21-builder-openshift-rhel9`
  - `registry.redhat.io/jboss-eap-8/eap81-openjdk21-runtime-openshift-rhel9`
    を利用するため

## 使い方

1. Red Hat レジストリへログイン

```bash
docker login registry.redhat.io
```

2. WAR を事前ビルド（プリビルド方式）

```bash
mvn -DskipTests package
```

3. イメージをビルドして起動

```bash
docker compose up --build -d
```

4. 動作確認

- トップページ: <http://localhost:8080/>
- `index.xhtml` 上で `Count Up` を押すと、`@SessionScoped` のカウンタが増えます
- 同じブラウザタブではカウンタが維持され、別ブラウザやシークレットウィンドウでは別セッションになります
- フォーム送信後は `faces-redirect=true` で `GET` に遷移する PRG パターン構成です

JSF 画面を利用するため、EAP の Galleon レイヤーに `jsf` を含めています。
`Dockerfile` 変更後は必ず `--no-cache` 付きで再ビルドしてください。
Docker build は `target/ROOT.war` をそのままコピーします。

## 停止

```bash
docker compose down
```
