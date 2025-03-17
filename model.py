from flask_sqlalchemy import SQLAlchemy

db = SQLAlchemy()

class Users(db.Model):
    __tablename__ = 'users'  # 테이블 이름을 지정
    email = db.Column(db.String(100), primary_key=True)  # Spring에서는 @Id로 설정, 여기서는 primary_key=True
    password = db.Column(db.String(255), nullable=False)
    nickname = db.Column(db.String(100), nullable=True)
    birth = db.Column(db.String(20), nullable=True)  # Spring에서는 String, 여기서는 varchar와 유사
    role = db.Column(db.String(50), nullable=True)
    membership = db.Column(db.Integer, nullable=True)

    def __repr__(self):
        return f"<User {self.email}>"


class AiTable(db.Model):
    __tablename__ = 'ai_table'  # 테이블 이름 설정

    # 필드 정의
    id = db.Column(db.Integer, primary_key=True, autoincrement=True)  # id 필드는 자동 증가
    email = db.Column(db.String(100), nullable=False)  # 이메일 필드
    change_Cnt = db.Column(db.Integer, default=0)  # changeCnt 필드
    recommand_Cnt = db.Column(db.Integer, default=0)  # recommandCnt 필드
    melody_Cnt = db.Column(db.Integer, default=0)  # melodyCnt 필드
    image_Cnt = db.Column(db.Integer, default=0)  # imageCnt 필드

    def __repr__(self):
        return f"<AiTable {self.email}>"


def is_change_able(email):
    user = Users.query.filter_by(email=email).first()
    ai_table = AiTable.query.filter_by(email=email).first()

    # 사용자가 존재하지 않으면 False 반환
    if not user or not ai_table:
        return False

    # 멤버십 가입자가 아니면 하루 제한 확인
    if user.membership == 1:
        return True
    else:
        if ai_table.image_Cnt < 10:
            ai_table.image_Cnt += 1
            db.session.commit()
            return True
        else:
            return False
