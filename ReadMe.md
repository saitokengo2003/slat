# Web アプリケーション構築 2 実習ワークスペース

## VSCode に最初にインストールする 拡張機能

### 入れ方

1. 左の「拡張機能」ボタンを押す
1. 検索ボックスに「@recommended」を入れる
1. 「ワークスペースの推奨事項」に表示されている Extension をインストール

## PostgreSQL DB への接続

### PostgreSQL 拡張機能の活用

1. VSCode 左側のアイコンの中から「Database」を開く

2. 「Create Connection」を押す

3. 「Connect」内で以下の設定を入力し、下部の「＋ Connect」ボタンを押す

- Name : DB
- Host : 127.0.0.1 ※デフォルト
- Port : 5432 ※デフォルト
- Username : postgres ※デフォルト
- Password : postgres
- Database : postgres ※デフォルト

4. PostgreSQL の DB に接続されれば準備完了

- ※エラーになった場合は、PostgreSQL のインストール状況を確認する

## Controller と Service で実装するロジックの責任分界点について

【引用】[3.2. ドメイン層の実装](https://terasolunaorg.github.io/guideline/current/ja/ImplementationAtEachLayer/DomainLayer.html#service)

本ガイドラインでは、Controller と Service で実装するロジックは、以下のルールに則って実装することを推奨する。

- クライアントからリクエストされたデータに対する単項目チェック、相関項目チェックは Controller 側(Bean Validation または Spring Validator)で行う。
- Service に渡すデータへの変換処理(Bean 変換、型変換、形式変換など)は、Service ではなく Controller 側で行う。
- ビジネスルールに関わる処理は Service で行う。業務データへのアクセスは、Repository または O/R Mapper に委譲する。
- Service から Controller に返却するデータ（クライアントへレスポンスするデータ）に対する値の変換処理(型変換、形式変換など)は、Service ではなく、Controller 側（View クラスなど）で行う。
