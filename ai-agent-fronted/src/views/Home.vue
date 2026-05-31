<template>
  <div class="home">
    <header class="home-header">
      <h1>AI Agent 应用中心</h1>
      <p>选择下方应用，开始你的 AI 对话体验</p>
    </header>

    <main class="app-grid">
      <router-link to="/love-app" class="app-card love">
        <div class="card-icon">💕</div>
        <h2>AI 恋爱大师</h2>
        <p>情感咨询与恋爱指导，温暖陪伴你的每一次对话</p>
        <span class="card-action">进入应用 →</span>
      </router-link>

      <router-link to="/manus-app" class="app-card manus">
        <div class="card-icon">🤖</div>
        <h2>AI 超级智能体</h2>
        <p>全能 AI 助手，帮你解决各类复杂问题与任务</p>
        <span class="card-action">进入应用 →</span>
      </router-link>
    </main>
  </div>
</template>

<style scoped>
.home {
  min-height: 100vh;
  padding: 48px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.home-header {
  text-align: center;
  color: #fff;
  margin-bottom: 48px;
}

.home-header h1 {
  font-size: 2.25rem;
  font-weight: 700;
  margin-bottom: 12px;
}

.home-header p {
  font-size: 1.05rem;
  opacity: 0.9;
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.app-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 28px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  transition: transform 0.2s, box-shadow 0.2s;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
}

.card-icon {
  font-size: 2.5rem;
}

.app-card h2 {
  font-size: 1.35rem;
  font-weight: 600;
  color: #1f2937;
}

.app-card p {
  font-size: 0.95rem;
  color: #6b7280;
  line-height: 1.6;
  flex: 1;
}

.card-action {
  font-size: 0.95rem;
  font-weight: 600;
  margin-top: 8px;
}

.app-card.love .card-action {
  color: #ec4899;
}

.app-card.manus .card-action {
  color: #6366f1;
}
</style>
