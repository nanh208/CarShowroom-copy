<template>
  <div class="vip-card-master-wrapper">
    <!-- Thẻ thành viên tương tác chính (Chuyển đổi linh hoạt theo Hạng: Đồng, Bạc, Vàng, Bạch Kim) -->
    <div
      class="vcb-platinum-card"
      :class="[
        'card-tier-' + tierKey,
        { 'is-clickable': allowClick }
      ]"
      role="button"
      tabindex="0"
      :aria-label="'Thẻ hội viên ' + tierDisplayName + ' - Nhấn để xem chi tiết'"
      @click="handleCardClick"
      @keydown.enter="handleCardClick"
      @keydown.space.prevent="handleCardClick"
    >
      <!-- Lớp phủ ánh kim satin quét động -->
      <div class="card-metallic-sheen" :class="'sheen-' + tierKey" />

      <!-- Cụm khối đa giác 3D kim loại (Faceted Triangular Prisms SVG biến đổi theo từng hạng) -->
      <div class="card-facets-overlay">
        <svg viewBox="0 0 600 378" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none">
          <defs>
            <!-- 1. HẠNG ĐỒNG (BRONZE) -->
            <linearGradient id="facet-g1-bronze" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#92400e"/><stop offset="50%" stop-color="#451a03"/><stop offset="100%" stop-color="#1f140e"/>
            </linearGradient>
            <linearGradient id="facet-g2-bronze" x1="100%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#f59e0b"/><stop offset="45%" stop-color="#b45309"/><stop offset="100%" stop-color="#78350f"/>
            </linearGradient>
            <linearGradient id="facet-g3-bronze" x1="0%" y1="100%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#451a03"/><stop offset="50%" stop-color="#78350f"/><stop offset="100%" stop-color="#b45309"/>
            </linearGradient>
            <linearGradient id="facet-g4-bronze" x1="50%" y1="0%" x2="50%" y2="100%">
              <stop offset="0%" stop-color="#fde68a"/><stop offset="40%" stop-color="#f59e0b"/><stop offset="100%" stop-color="#78350f"/>
            </linearGradient>
            <linearGradient id="facet-g5-bronze" x1="0%" y1="50%" x2="100%" y2="50%">
              <stop offset="0%" stop-color="#1c1109"/><stop offset="70%" stop-color="#451a03"/><stop offset="100%" stop-color="#291509"/>
            </linearGradient>
            <linearGradient id="facet-highlight-bronze" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#fffbeb"/><stop offset="35%" stop-color="#fed7aa"/><stop offset="100%" stop-color="#d97706"/>
            </linearGradient>
            <linearGradient id="facet-dark-bronze" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#291509"/><stop offset="100%" stop-color="#140903"/>
            </linearGradient>

            <!-- 2. HẠNG BẠC (SILVER - ÁNH THÉP BẠC SÁNG SẮC NÉT) -->
            <linearGradient id="facet-g1-silver" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#94a3b8"/><stop offset="50%" stop-color="#475569"/><stop offset="100%" stop-color="#334155"/>
            </linearGradient>
            <linearGradient id="facet-g2-silver" x1="100%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#f8fafc"/><stop offset="45%" stop-color="#cbd5e1"/><stop offset="100%" stop-color="#94a3b8"/>
            </linearGradient>
            <linearGradient id="facet-g3-silver" x1="0%" y1="100%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#475569"/><stop offset="50%" stop-color="#64748b"/><stop offset="100%" stop-color="#cbd5e1"/>
            </linearGradient>
            <linearGradient id="facet-g4-silver" x1="50%" y1="0%" x2="50%" y2="100%">
              <stop offset="0%" stop-color="#ffffff"/><stop offset="40%" stop-color="#e2e8f0"/><stop offset="100%" stop-color="#94a3b8"/>
            </linearGradient>
            <linearGradient id="facet-g5-silver" x1="0%" y1="50%" x2="100%" y2="50%">
              <stop offset="0%" stop-color="#1e293b"/><stop offset="70%" stop-color="#334155"/><stop offset="100%" stop-color="#475569"/>
            </linearGradient>
            <linearGradient id="facet-highlight-silver" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#ffffff"/><stop offset="35%" stop-color="#f8fafc"/><stop offset="100%" stop-color="#cbd5e1"/>
            </linearGradient>
            <linearGradient id="facet-dark-silver" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#334155"/><stop offset="100%" stop-color="#1e293b"/>
            </linearGradient>

            <!-- 3. HẠNG VÀNG (GOLD) -->
            <linearGradient id="facet-g1-gold" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#854d0e"/><stop offset="50%" stop-color="#713f12"/><stop offset="100%" stop-color="#2e1e05"/>
            </linearGradient>
            <linearGradient id="facet-g2-gold" x1="100%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#fde047"/><stop offset="45%" stop-color="#eab308"/><stop offset="100%" stop-color="#ca8a04"/>
            </linearGradient>
            <linearGradient id="facet-g3-gold" x1="0%" y1="100%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#713f12"/><stop offset="50%" stop-color="#a16207"/><stop offset="100%" stop-color="#eab308"/>
            </linearGradient>
            <linearGradient id="facet-g4-gold" x1="50%" y1="0%" x2="50%" y2="100%">
              <stop offset="0%" stop-color="#fef08a"/><stop offset="40%" stop-color="#facc15"/><stop offset="100%" stop-color="#ca8a04"/>
            </linearGradient>
            <linearGradient id="facet-g5-gold" x1="0%" y1="50%" x2="100%" y2="50%">
              <stop offset="0%" stop-color="#2e1e05"/><stop offset="70%" stop-color="#422b07"/><stop offset="100%" stop-color="#1c1202"/>
            </linearGradient>
            <linearGradient id="facet-highlight-gold" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#ffffff"/><stop offset="35%" stop-color="#fef9c3"/><stop offset="100%" stop-color="#facc15"/>
            </linearGradient>
            <linearGradient id="facet-dark-gold" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#2e1e05"/><stop offset="100%" stop-color="#1c1202"/>
            </linearGradient>

            <!-- 4. HẠNG TITAN (MÀU ĐEN TUYỀN SIÊU SANG - OBSIDIAN PITCH BLACK) -->
            <linearGradient id="facet-g1-titan" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#1c1c20"/><stop offset="50%" stop-color="#0c0c0e"/><stop offset="100%" stop-color="#020202"/>
            </linearGradient>
            <linearGradient id="facet-g2-titan" x1="100%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#2a2a30"/><stop offset="45%" stop-color="#161619"/><stop offset="100%" stop-color="#070708"/>
            </linearGradient>
            <linearGradient id="facet-g3-titan" x1="0%" y1="100%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#080809"/><stop offset="50%" stop-color="#18181b"/><stop offset="100%" stop-color="#222227"/>
            </linearGradient>
            <linearGradient id="facet-g4-titan" x1="50%" y1="0%" x2="50%" y2="100%">
              <stop offset="0%" stop-color="#383840"/><stop offset="40%" stop-color="#1f1f24"/><stop offset="100%" stop-color="#0a0a0c"/>
            </linearGradient>
            <linearGradient id="facet-g5-titan" x1="0%" y1="50%" x2="100%" y2="50%">
              <stop offset="0%" stop-color="#030304"/><stop offset="70%" stop-color="#0e0e11"/><stop offset="100%" stop-color="#000000"/>
            </linearGradient>
            <linearGradient id="facet-highlight-titan" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#73737c"/><stop offset="25%" stop-color="#3a3a42"/><stop offset="100%" stop-color="#121215"/>
            </linearGradient>
            <linearGradient id="facet-dark-titan" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#0a0a0c"/><stop offset="100%" stop-color="#000000"/>
            </linearGradient>

            <!-- Tương thích fallback Platinum -> Titan -->
            <linearGradient id="facet-g1-platinum" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#1c1c20"/><stop offset="50%" stop-color="#0c0c0e"/><stop offset="100%" stop-color="#020202"/>
            </linearGradient>
            <linearGradient id="facet-g2-platinum" x1="100%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#2a2a30"/><stop offset="45%" stop-color="#161619"/><stop offset="100%" stop-color="#070708"/>
            </linearGradient>
            <linearGradient id="facet-g3-platinum" x1="0%" y1="100%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#080809"/><stop offset="50%" stop-color="#18181b"/><stop offset="100%" stop-color="#222227"/>
            </linearGradient>
            <linearGradient id="facet-g4-platinum" x1="50%" y1="0%" x2="50%" y2="100%">
              <stop offset="0%" stop-color="#383840"/><stop offset="40%" stop-color="#1f1f24"/><stop offset="100%" stop-color="#0a0a0c"/>
            </linearGradient>
            <linearGradient id="facet-g5-platinum" x1="0%" y1="50%" x2="100%" y2="50%">
              <stop offset="0%" stop-color="#030304"/><stop offset="70%" stop-color="#0e0e11"/><stop offset="100%" stop-color="#000000"/>
            </linearGradient>
            <linearGradient id="facet-highlight-platinum" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#73737c"/><stop offset="25%" stop-color="#3a3a42"/><stop offset="100%" stop-color="#121215"/>
            </linearGradient>
            <linearGradient id="facet-dark-platinum" x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stop-color="#0a0a0c"/><stop offset="100%" stop-color="#000000"/>
            </linearGradient>
          </defs>

          <!-- Cụm đa giác góc trên bên trái -->
          <g class="corner-facets-top-left" :stroke="facetStrokeColor" stroke-width="0.75" stroke-linejoin="round">
            <polygon points="0,0 110,0 55,75" :fill="'url(#facet-g1-' + tierKey + ')'" />
            <polygon points="0,0 55,75 0,105" :fill="'url(#facet-g3-' + tierKey + ')'" />
            <polygon points="0,105 55,75 36,170" :fill="'url(#facet-g2-' + tierKey + ')'" />
            <polygon points="0,105 36,170 0,200" :fill="'url(#facet-g5-' + tierKey + ')'" />
            <polygon points="0,200 36,170 0,245" :fill="'url(#facet-dark-' + tierKey + ')'" />
            
            <polygon points="110,0 200,0 148,70" :fill="'url(#facet-g2-' + tierKey + ')'" />
            <polygon points="110,0 148,70 55,75" :fill="'url(#facet-highlight-' + tierKey + ')'" />
            <polygon points="55,75 148,70 108,142" :fill="'url(#facet-g4-' + tierKey + ')'" />
            <polygon points="55,75 108,142 36,170" :fill="'url(#facet-g1-' + tierKey + ')'" />
            
            <polygon points="200,0 280,0 238,60" :fill="'url(#facet-g3-' + tierKey + ')'" />
            <polygon points="200,0 238,60 148,70" :fill="'url(#facet-dark-' + tierKey + ')'" />
            <polygon points="148,70 238,60 196,128" :fill="'url(#facet-g2-' + tierKey + ')'" />
            <polygon points="148,70 196,128 108,142" :fill="'url(#facet-highlight-' + tierKey + ')'" />
            <polygon points="108,142 196,128 152,188" :fill="'url(#facet-g3-' + tierKey + ')'" />
            <polygon points="108,142 152,188 80,200" :fill="'url(#facet-g2-' + tierKey + ')'" />
            <polygon points="36,170 108,142 80,200" :fill="'url(#facet-g4-' + tierKey + ')'" />
            
            <polygon points="238,60 295,42 276,102" :fill="'url(#facet-g4-' + tierKey + ')'" />
            <polygon points="238,60 276,102 196,128" :fill="'url(#facet-g5-' + tierKey + ')'" />
            <polygon points="280,0 315,0 295,42" :fill="'url(#facet-g1-' + tierKey + ')'" />
          </g>

          <!-- Cụm đa giác góc giữa bên phải -->
          <g class="corner-facets-right" :stroke="facetStrokeColor" stroke-width="0.75" stroke-linejoin="round">
            <polygon points="460,135 535,95 496,168" :fill="'url(#facet-g2-' + tierKey + ')'" />
            <polygon points="535,95 595,128 496,168" :fill="'url(#facet-g1-' + tierKey + ')'" />
            <polygon points="496,168 595,128 568,205" :fill="'url(#facet-highlight-' + tierKey + ')'" />
            <polygon points="496,168 568,205 512,230" :fill="'url(#facet-g3-' + tierKey + ')'" />
            <polygon points="460,135 496,168 432,195" :fill="'url(#facet-g4-' + tierKey + ')'" />
            <polygon points="432,195 496,168 512,230" :fill="'url(#facet-g5-' + tierKey + ')'" />
            <polygon points="432,195 512,230 448,245" :fill="'url(#facet-g2-' + tierKey + ')'" />
          </g>
        </svg>
      </div>

      <!-- Hàng trên: Brand & Avatar thành viên -->
      <div class="card-header-row">
        <div class="header-left-space">
          <span class="card-tier-pill-badge" :class="'badge-' + tierKey">
            {{ tierDisplayName.toUpperCase() }} VIP
          </span>
        </div>
        <div class="header-right-group">
          <!-- Avatar thành viên với viền kim loại & seal VIP -->
          <div v-if="showAvatarBadge" class="member-avatar-medallion">
            <div class="medallion-ring" :class="'ring-' + tierKey">
              <img
                v-if="userAvatar"
                :src="userAvatar"
                class="medallion-img"
                alt="Member Avatar"
              />
              <div v-else class="medallion-fallback">
                {{ userInitial }}
              </div>
            </div>
            <span class="medallion-vip-seal" :class="'seal-' + tierKey">VIP</span>
          </div>
        </div>
      </div>

      <!-- Khu vực trung tâm: Chip EMV & Thư pháp theo từng hạng -->
      <div class="card-mid-section">
        <!-- Chip EMV kim loại sáng bóng -->
        <div class="silver-emv-chip" :class="'chip-' + tierKey">
          <div class="chip-slot slot-h1" />
          <div class="chip-slot slot-h2" />
          <div class="chip-slot slot-v1" />
          <div class="chip-slot slot-v2" />
          <div class="chip-center-chamber" />
        </div>

        <!-- Chữ thư pháp uốn lượn trung tâm (Bronze / Silver / Gold / Platinum) -->
        <div class="calligraphy-platinum-wrap" :class="'calligraphy-' + tierKey">
          <span class="calligraphy-platinum-text">{{ tierCalligraphy }}</span>
        </div>
      </div>

      <!-- Hàng dưới: Số thẻ dập nổi 3D & Hạn thẻ -->
      <div class="card-footer-section">
        <!-- Dãy số thẻ dập nổi 3D ánh kim -->
        <div class="embossed-card-number" :class="'embossed-' + tierKey">
          {{ formattedCardNumber }}
        </div>

        <div class="card-metadata-grid">
          <div class="meta-col-cardholder">
            <div class="meta-subline">
              <span class="prefix-embossed">{{ cardPrefix }}</span>
              <div class="valid-thru-group">
                <div class="vt-headers">
                  <span>VALID THRU</span>
                  <span class="vt-my">MONTH/YEAR</span>
                </div>
                <span class="vt-digits">{{ validThru }}</span>
              </div>
            </div>
            <div class="meta-owner-name">
              {{ cardHolderName }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- MODAL CHI TIẾT THẺ HỘI VIÊN (Đồng, Bạc, Vàng, Bạch Kim) -->
    <Teleport to="body">
      <div v-if="showModal" class="card-modal-backdrop" @click.self="closeModal">
        <div class="card-modal-container" role="dialog" aria-modal="true">
          <!-- Modal Header -->
          <div class="card-modal-header">
            <div class="cm-header-titles">
              <span class="cm-tag">ARES RACING CLUB &bull; OFFICIAL PASSPORT</span>
              <h2 class="cm-title">CHI TIẾT THẺ HỘI VIÊN {{ tierDisplayName.toUpperCase() }}</h2>
              <p class="cm-desc">Định danh hội viên cao cấp · Tích hợp thanh toán và quyền lợi đặc quyền</p>
            </div>
            <button class="cm-close-btn" aria-label="Đóng" @click="closeModal">&times;</button>
          </div>

          <div class="card-modal-body">
            <!-- Khu vực thẻ 3D lật 2 mặt (Interactive 3D Flip Card) -->
            <div class="flip-card-stage">
              <div class="flip-card-box" :class="{ 'is-flipped': isFlipped }">
                <!-- Mặt trước (Front) -->
                <div class="flip-side flip-front">
                  <div class="vcb-platinum-card preview-card-mode" :class="'card-tier-' + tierKey">
                    <div class="card-metallic-sheen" :class="'sheen-' + tierKey" />
                    <!-- SVG Facets Overlay -->
                    <div class="card-facets-overlay">
                      <svg viewBox="0 0 600 378" width="100%" height="100%" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none">
                        <g class="corner-facets-top-left" :stroke="facetStrokeColor" stroke-width="0.75" stroke-linejoin="round">
                          <polygon points="0,0 110,0 55,75" :fill="'url(#facet-g1-' + tierKey + ')'" />
                          <polygon points="0,0 55,75 0,105" :fill="'url(#facet-g3-' + tierKey + ')'" />
                          <polygon points="0,105 55,75 36,170" :fill="'url(#facet-g2-' + tierKey + ')'" />
                          <polygon points="0,105 36,170 0,200" :fill="'url(#facet-g5-' + tierKey + ')'" />
                          <polygon points="110,0 200,0 148,70" :fill="'url(#facet-g2-' + tierKey + ')'" />
                          <polygon points="110,0 148,70 55,75" :fill="'url(#facet-highlight-' + tierKey + ')'" />
                          <polygon points="55,75 148,70 108,142" :fill="'url(#facet-g4-' + tierKey + ')'" />
                          <polygon points="200,0 280,0 238,60" :fill="'url(#facet-g3-' + tierKey + ')'" />
                          <polygon points="148,70 238,60 196,128" :fill="'url(#facet-g2-' + tierKey + ')'" />
                          <polygon points="108,142 196,128 152,188" :fill="'url(#facet-g3-' + tierKey + ')'" />
                        </g>
                        <g class="corner-facets-right" :stroke="facetStrokeColor" stroke-width="0.75" stroke-linejoin="round">
                          <polygon points="460,135 535,95 496,168" :fill="'url(#facet-g2-' + tierKey + ')'" />
                          <polygon points="535,95 595,128 496,168" :fill="'url(#facet-g1-' + tierKey + ')'" />
                          <polygon points="496,168 595,128 568,205" :fill="'url(#facet-highlight-' + tierKey + ')'" />
                          <polygon points="496,168 568,205 512,230" :fill="'url(#facet-g3-' + tierKey + ')'" />
                        </g>
                      </svg>
                    </div>

                    <div class="card-header-row">
                      <div class="header-left-space">
                        <span class="card-tier-pill-badge" :class="'badge-' + tierKey">
                          {{ tierDisplayName.toUpperCase() }} VIP
                        </span>
                      </div>
                      <div class="header-right-group">
                        <div v-if="showAvatarBadge" class="member-avatar-medallion">
                          <div class="medallion-ring" :class="'ring-' + tierKey">
                            <img v-if="userAvatar" :src="userAvatar" class="medallion-img" alt="Member Avatar" />
                            <div v-else class="medallion-fallback">{{ userInitial }}</div>
                          </div>
                        </div>
                      </div>
                    </div>

                    <div class="card-mid-section">
                      <div class="silver-emv-chip" :class="'chip-' + tierKey">
                        <div class="chip-slot slot-h1" /><div class="chip-slot slot-h2" />
                        <div class="chip-center-chamber" />
                      </div>
                      <div class="calligraphy-platinum-wrap" :class="'calligraphy-' + tierKey">
                        <span class="calligraphy-platinum-text">{{ tierCalligraphy }}</span>
                      </div>
                    </div>

                    <div class="card-footer-section">
                      <div class="embossed-card-number" :class="'embossed-' + tierKey">{{ formattedCardNumber }}</div>
                      <div class="card-metadata-grid">
                        <div class="meta-col-cardholder">
                          <div class="meta-subline">
                            <span class="prefix-embossed">{{ cardPrefix }}</span>
                            <div class="valid-thru-group">
                              <div class="vt-headers"><span>VALID THRU</span><span class="vt-my">MONTH/YEAR</span></div>
                              <span class="vt-digits">{{ validThru }}</span>
                            </div>
                          </div>
                          <div class="meta-owner-name">{{ cardHolderName }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- Mặt sau (Back) -->
                <div class="flip-side flip-back">
                  <div class="card-back-container" :class="'back-tier-' + tierKey">
                    <!-- Dải từ đen -->
                    <div class="magnetic-stripe" />

                    <!-- Dải chữ ký và mã CVV bảo mật -->
                    <div class="signature-row">
                      <div class="signature-panel">
                        <span class="sig-pattern">ARES RACING CLUB SECURITY &bull; SIGNATURE REQUIRED</span>
                      </div>
                      <div class="cvv-panel">
                        <span class="cvv-label">CVV / CVC</span>
                        <div class="cvv-value-wrap">
                          <b class="cvv-number">{{ showCvv ? '888' : '&bull;&bull;&bull;' }}</b>
                          <button class="cvv-toggle" @click="showCvv = !showCvv" :title="showCvv ? 'Ẩn CVV' : 'Hiện CVV'">
                            {{ showCvv ? 'Ẩn' : 'Hiện' }}
                          </button>
                        </div>
                      </div>
                    </div>

                    <!-- Hologram và mã QR định danh tại Showroom -->
                    <div class="back-meta-grid">
                      <div class="hologram-seal" :class="'holo-' + tierKey">
                        <div class="holo-shimmer" />
                        <span class="holo-text">ARES VIP<br/>{{ tierDisplayName.toUpperCase() }}</span>
                      </div>

                      <div class="showroom-qr-block">
                        <div class="qr-box">
                          <svg viewBox="0 0 24 24" width="62" height="62" fill="currentColor">
                            <path d="M2 2h8v8H2V2zm2 2v4h4V4H4zm10-2h8v8h-8V2zm2 2v4h4V4h-4zM2 14h8v8H2v-8zm2 2v4h4v-4H4zm14 2h4v4h-4v-4zm-4-4h4v2h-4v-2zm2 4h2v2h-2v-2zm-2 2h2v2h-2v-2zm4-2h2v4h-2v-4zm0-4h4v2h-4v-2zm-6-2h2v2h-2v-2z"/>
                          </svg>
                        </div>
                        <span class="qr-sub">Mã quét VIP tại quầy</span>
                      </div>
                    </div>

                    <!-- Chân trang thẻ mặt sau -->
                    <div class="back-footer-text">
                      <span>Đường dây nóng VIP 24/7: 1900 8888 &bull; https://ares.toys</span>
                      <span>Thẻ này là tài sản của ARES RACING CLUB. Nhặt được xin vui lòng hoàn trả showroom gần nhất.</span>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Nút điều khiển lật thẻ -->
              <div class="flip-control-bar">
                <button class="btn btn-outline btn-sm flip-btn" @click="isFlipped = !isFlipped">
                  <svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 10c0-4.4-3.6-8-8-8s-8 3.6-8 8h-3l4 4 4-4h-3c0-3.3 2.7-6 6-6s6 2.7 6 6-2.7 6-6 6v2c4.4 0 8-3.6 8-8z"/>
                  </svg>
                  <span>{{ isFlipped ? 'Xem mặt trước thẻ' : 'Lật xem mặt sau (Dải từ & CVV)' }}</span>
                </button>
              </div>
            </div>

            <!-- Bảng thông tin định danh thẻ -->
            <div class="card-details-info-section">
              <h3 class="info-section-title">HỒ SƠ HỘI VIÊN ĐIỆN TỬ</h3>
              <div class="details-specs-grid">
                <div class="spec-card">
                  <span class="spec-lbl">Mã số thẻ VIP:</span>
                  <div class="spec-val-row">
                    <b class="spec-val-mono">{{ formattedCardNumber }}</b>
                    <button class="btn btn-sm btn-outline btn-copy-action" @click="copyCardNumber">
                      {{ copied ? 'Đã chép' : 'Sao chép' }}
                    </button>
                  </div>
                </div>

                <div class="spec-card">
                  <span class="spec-lbl">Hạng thẻ hội viên:</span>
                  <b class="spec-val-tier" :class="'tier-text-' + tierKey">
                    {{ tierDisplayName }} VIP ({{ tierCalligraphy }} Privilege)
                  </b>
                </div>

                <div class="spec-card">
                  <span class="spec-lbl">Chủ sở hữu định danh:</span>
                  <b class="spec-val-name">{{ cardHolderName }}</b>
                </div>

                <div class="spec-card">
                  <span class="spec-lbl">Thời hạn hiệu lực:</span>
                  <b class="spec-val-valid">{{ validThru }} (Tự động gia hạn)</b>
                </div>
              </div>

              <!-- Danh mục đặc quyền theo đúng hạng hiện tại -->
              <h3 class="info-section-title" style="margin-top: 18px">ĐẶC QUYỀN ĐỘC QUYỀN HẠNG {{ tierDisplayName.toUpperCase() }}</h3>
              <div class="perks-list-grid">
                <div v-for="p in currentTierPerks" :key="p.title" class="perk-box">
                  <div class="perk-badge-dot" :class="'dot-' + tierKey" />
                  <div>
                    <b>{{ p.title }}</b>
                    <p>{{ p.desc }}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- Modal Footer -->
          <div class="card-modal-footer">
            <button class="btn btn-outline" @click="closeModal">Đóng</button>
            <button class="btn btn-primary" @click="copyCardNumber">
              {{ copied ? 'Đã sao chép số thẻ!' : 'Sao chép mã thẻ VIP' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  tier: { type: String, default: 'Đồng' },
  points: { type: Number, default: 0 },
  user: { type: Object, default: () => ({}) },
  allowClick: { type: Boolean, default: true },
  showAvatarBadge: { type: Boolean, default: true }
})

const showModal = ref(false)
const isFlipped = ref(false)
const showCvv = ref(false)
const copied = ref(false)

const tierKey = computed(() => {
  const raw = (props.tier || '').toLowerCase().trim()
  // Ưu tiên kiểm tra Titan / Bạch Kim trước để 'bạch kim' không bị dính 'bạc' (vì 'bạch' chứa 'bạc')
  if (raw.includes('titan') || raw.includes('bạch') || raw.includes('bach') || raw.includes('platinum') || raw.includes('kim cương') || raw.includes('diamond')) {
    return 'titan'
  }
  if (raw.includes('vàng') || raw.includes('vang') || raw.includes('gold')) return 'gold'
  if (raw.includes('đồng') || raw.includes('dong') || raw.includes('bronze')) return 'bronze'
  if (raw.includes('bạc') || raw.includes('bac') || raw.includes('silver')) return 'silver'
  return 'titan'
})

const tierDisplayName = computed(() => {
  switch (tierKey.value) {
    case 'bronze': return 'Đồng'
    case 'silver': return 'Bạc'
    case 'gold': return 'Vàng'
    case 'titan':
    default: return 'Titan'
  }
})

const tierCalligraphy = computed(() => {
  switch (tierKey.value) {
    case 'bronze': return 'Bronze'
    case 'silver': return 'Silver'
    case 'gold': return 'Gold'
    case 'titan':
    default: return 'Titanium'
  }
})

const facetStrokeColor = computed(() => {
  switch (tierKey.value) {
    case 'bronze': return '#451a03'
    case 'silver': return '#475569'
    case 'gold': return '#422b07'
    case 'titan':
    default: return 'rgba(255, 255, 255, 0.12)'
  }
})

const TIER_PERKS_MAP = {
  bronze: [
    { title: 'Tích lũy 1% giá trị mọi đơn hàng thành điểm', desc: 'Quy đổi 1 điểm = 100đ giảm giá trực tiếp vào đơn hàng kế tiếp.' },
    { title: 'Quà tặng sinh nhật thành viên 50.000đ', desc: 'Voucher gửi trực tiếp vào ví quà tặng trong tháng sinh nhật của bạn.' },
    { title: 'Miễn phí đóng gói hộp quà cao cấp', desc: 'Đóng gói chuẩn ARES sang trọng kèm thiệp viết tay theo yêu cầu.' },
    { title: 'Bảo hành chính hãng toàn diện', desc: 'Hỗ trợ kiểm định xe mô hình và linh kiện phụ kiện tiêu chuẩn.' }
  ],
  silver: [
    { title: 'Tích lũy 2% giá trị đơn hàng thành điểm thưởng', desc: 'Tốc độ tích điểm gấp đôi so với hạng tiêu chuẩn.' },
    { title: '5 Voucher Freeship mỗi tháng', desc: 'Tự động cộng vào ví voucher vào ngày 01 hằng tháng.' },
    { title: 'Ưu tiên đóng gói và xử lý giao hàng', desc: 'Đơn hàng được phân luồng xử lý trước tại tổng kho ARES.' },
    { title: 'Quà tặng sinh nhật VIP 100.000đ', desc: 'Phiếu quà tặng đặc biệt mừng sinh nhật hội viên Bạc.' }
  ],
  gold: [
    { title: 'Tích lũy 3% giá trị đơn hàng thành điểm thưởng', desc: 'Nhận điểm thưởng tối ưu cho mọi đơn xe mô hình và xe RC.' },
    { title: 'Miễn phí vận chuyển toàn quốc cho đơn từ 300.000đ', desc: 'Áp dụng mọi hình thức chuyển phát nhanh toàn quốc.' },
    { title: 'Quyền ưu tiên đấu giá xe mô hình hiếm', desc: 'Được mời tham gia các phiên đấu giá kín các mẫu xe độc bản.' },
    { title: 'Quà tặng sinh nhật VIP 200.000đ', desc: 'Quà tặng sinh nhật cao cấp cùng thiệp chúc mừng riêng.' },
    { title: 'Chính sách đổi trả miễn phí trong 14 ngày', desc: 'Đổi mới không phát sinh chi phí nếu không hài lòng với sản phẩm.' }
  ],
  titan: [
    { title: 'Tích lũy tối đa 5% giá trị mọi đơn hàng', desc: 'Cấp bậc tích điểm cao nhất trong toàn bộ hệ thống ARES.' },
    { title: 'Miễn phí giao hàng hỏa tốc toàn quốc không giới hạn', desc: 'Nhận xe ngay trong ngày tại nội thành, hỗ trợ hỏa tốc liên tỉnh.' },
    { title: 'Suất đặt trước xe Limited số seri 1-10', desc: 'Ưu tiên sở hữu các mẫu xe đúc giới hạn trước khi công bố ra thị trường.' },
    { title: 'Quà tặng sinh nhật thượng hạng 500.000đ', desc: 'Món quà đặc biệt dành riêng cho hội viên Titan cao cấp nhất.' },
    { title: 'Bảo hành VIP 1 đổi 1 tận nơi trong 30 ngày', desc: 'Kỹ thuật viên phục vụ tận nhà trong khu vực nội thành.' },
    { title: 'Trợ lý riêng phục vụ 24/7 & Hỗ trợ kỹ thuật VIP', desc: 'Kênh liên lạc độc quyền tư vấn phụ kiện và bảo dưỡng định kỳ.' }
  ]
}
TIER_PERKS_MAP.platinum = TIER_PERKS_MAP.titan

const currentTierPerks = computed(() => TIER_PERKS_MAP[tierKey.value] || TIER_PERKS_MAP.titan)

const userAvatar = computed(() => props.user?.avatarUrl || props.user?.avatar || '')
const userInitial = computed(() => (props.user?.fullName || 'A').charAt(0).toUpperCase())
const cardHolderName = computed(() => (props.user?.fullName || 'QUẢN TRỊ VIÊN').toUpperCase())
const validThru = ref('12/28')

const formattedCardNumber = computed(() => {
  const numOrId = props.user?.vipCardNumber || props.user?.id
  if (numOrId && typeof numOrId === 'string' && numOrId.length >= 12) {
    return numOrId.replace(/[-]/g, '   ')
  }
  const idStr = String(numOrId || 1).padStart(4, '0')
  const ptsSuffix = String((props.points || 0) + 1200).padStart(4, '0')
  return `8899   2026   ${idStr}   ${ptsSuffix}`
})

const cardPrefix = computed(() => {
  return formattedCardNumber.value.slice(0, 4)
})

function handleCardClick() {
  if (props.allowClick) {
    showModal.value = true
    isFlipped.value = false
  }
}

function closeModal() {
  showModal.value = false
  isFlipped.value = false
  showCvv.value = false
}

async function copyCardNumber() {
  try {
    const rawDigits = formattedCardNumber.value.replace(/\s+/g, '')
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(rawDigits)
    }
    copied.value = true
    setTimeout(() => { copied.value = false }, 2500)
  } catch (err) {
    copied.value = true
    setTimeout(() => { copied.value = false }, 2000)
  }
}
</script>

<style scoped>
/* ==========================================================================
   ARES VIP PASSPORT MULTI-TIER LUXURY CARD DESIGN SYSTEM
   ========================================================================== */

.vip-card-master-wrapper {
  width: 100%;
  max-width: 560px;
  margin: 0 auto;
}

/* Thẻ chính tỉ lệ chuẩn ISO/IEC 7810 ID-1 (1.586 : 1) */
.vcb-platinum-card {
  width: 100%;
  aspect-ratio: 1.586 / 1;
  min-height: 295px;
  border-radius: 18px !important;
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 22px 26px 20px;
  color: #fff;
  overflow: hidden;
  user-select: none;
  transition: transform 0.35s cubic-bezier(0.2, 0.8, 0.2, 1),
              box-shadow 0.35s cubic-bezier(0.2, 0.8, 0.2, 1),
              background 0.4s ease,
              border-color 0.4s ease;
}

/* 1. THEME HẠNG ĐỒNG (BRONZE) */
.card-tier-bronze {
  background: radial-gradient(circle at 85% 15%, rgba(251, 146, 60, 0.22) 0%, transparent 45%),
              radial-gradient(circle at 15% 85%, rgba(180, 83, 9, 0.18) 0%, transparent 50%),
              linear-gradient(135deg, #1b0e06 0%, #2f170a 28%, #4a2610 52%, #261308 78%, #140903 100%);
  border: 1.5px solid rgba(245, 158, 11, 0.45);
  box-shadow: 0 28px 60px -12px rgba(0, 0, 0, 0.95),
              0 0 0 1px rgba(245, 158, 11, 0.15),
              inset 0 1px 2px rgba(254, 215, 170, 0.35);
}
.card-tier-bronze.is-clickable:hover {
  transform: translateY(-5px) scale(1.015);
  box-shadow: 0 36px 75px -12px rgba(0, 0, 0, 0.98),
              0 0 0 1.5px rgba(245, 158, 11, 0.55),
              0 0 26px rgba(217, 119, 6, 0.3);
  border-color: rgba(251, 146, 60, 0.6);
}
.card-tier-bronze .calligraphy-platinum-text {
  background: linear-gradient(180deg, #ffffff 0%, #fed7aa 20%, #f59e0b 45%, #b45309 60%, #fed7aa 80%, #78350f 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.95)) drop-shadow(0 0 16px rgba(245, 158, 11, 0.35));
}
.card-tier-bronze .embossed-card-number {
  background: linear-gradient(180deg, #ffffff 0%, #fed7aa 35%, #d97706 55%, #fde68a 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(1px 2px 2px rgba(0, 0, 0, 0.95)) drop-shadow(-0.8px -0.8px 0.5px rgba(254, 215, 170, 0.6));
}
.card-tier-bronze .silver-emv-chip {
  background: linear-gradient(135deg, #fef3c7 0%, #fed7aa 25%, #d97706 60%, #b45309 85%, #78350f 100%);
  border-color: #92400e;
}
.card-tier-bronze .medallion-ring {
  background: linear-gradient(135deg, #fde68a 0%, #f59e0b 40%, #78350f 100%);
}
.card-tier-bronze .medallion-vip-seal {
  background: linear-gradient(135deg, #f59e0b 0%, #92400e 100%);
  color: #fff;
}

/* 2. THEME HẠNG BẠC (SILVER - ÁNH KIM BẠC SÁNG) */
.card-tier-silver {
  background: radial-gradient(circle at 85% 15%, rgba(255, 255, 255, 0.35) 0%, transparent 45%),
              radial-gradient(circle at 15% 85%, rgba(148, 163, 184, 0.25) 0%, transparent 50%),
              linear-gradient(135deg, #1e2633 0%, #2b3545 28%, #415065 52%, #293444 78%, #161d27 100%);
  border: 1.5px solid rgba(226, 232, 240, 0.6);
  box-shadow: 0 28px 60px -12px rgba(0, 0, 0, 0.95),
              0 0 0 1px rgba(226, 232, 240, 0.25),
              inset 0 1px 2px rgba(255, 255, 255, 0.5);
}
.card-tier-silver.is-clickable:hover {
  transform: translateY(-5px) scale(1.015);
  box-shadow: 0 36px 75px -12px rgba(0, 0, 0, 0.98),
              0 0 0 1.5px rgba(255, 255, 255, 0.65),
              0 0 28px rgba(203, 213, 225, 0.4);
  border-color: rgba(255, 255, 255, 0.8);
}
.card-tier-silver .calligraphy-platinum-text {
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 24%, #cbd5e1 46%, #64748b 52%, #f1f5f9 76%, #475569 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.95)) drop-shadow(0 0 16px rgba(255, 255, 255, 0.35));
}
.card-tier-silver .embossed-card-number {
  background: linear-gradient(180deg, #ffffff 0%, #f1f5f9 35%, #94a3b8 55%, #e2e8f0 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(1px 2px 2px rgba(0, 0, 0, 0.95)) drop-shadow(-0.8px -0.8px 0.5px rgba(255, 255, 255, 0.7));
}
.card-tier-silver .silver-emv-chip {
  background: linear-gradient(135deg, #ffffff 0%, #e2e8f0 30%, #94a3b8 60%, #cbd5e1 85%, #64748b 100%);
  border-color: #475569;
}
.card-tier-silver .medallion-ring {
  background: linear-gradient(135deg, #ffffff 0%, #cbd5e1 40%, #64748b 100%);
}
.card-tier-silver .medallion-vip-seal {
  background: linear-gradient(135deg, #e2e8f0 0%, #94a3b8 100%);
  color: #0f172a;
}

/* 3. THEME HẠNG VÀNG (GOLD) */
.card-tier-gold {
  background: radial-gradient(circle at 85% 15%, rgba(250, 204, 21, 0.28) 0%, transparent 45%),
              radial-gradient(circle at 15% 85%, rgba(202, 138, 4, 0.2) 0%, transparent 50%),
              linear-gradient(135deg, #1c1402 0%, #302305 28%, #4a3708 52%, #291d04 78%, #100b01 100%);
  border: 1.5px solid rgba(234, 179, 8, 0.55);
  box-shadow: 0 28px 60px -12px rgba(0, 0, 0, 0.95),
              0 0 0 1px rgba(234, 179, 8, 0.2),
              inset 0 1px 2px rgba(254, 240, 138, 0.4);
}
.card-tier-gold.is-clickable:hover {
  transform: translateY(-5px) scale(1.015);
  box-shadow: 0 36px 75px -12px rgba(0, 0, 0, 0.98),
              0 0 0 1.5px rgba(250, 204, 21, 0.65),
              0 0 28px rgba(234, 179, 8, 0.35);
  border-color: rgba(250, 204, 21, 0.7);
}
.card-tier-gold .calligraphy-platinum-text {
  background: linear-gradient(180deg, #ffffff 0%, #fef08a 22%, #facc15 45%, #ca8a04 60%, #fef9c3 80%, #854d0e 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.95)) drop-shadow(0 0 18px rgba(250, 204, 21, 0.4));
}
.card-tier-gold .embossed-card-number {
  background: linear-gradient(180deg, #ffffff 0%, #fef08a 35%, #eab308 55%, #fef9c3 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(1px 2px 2px rgba(0, 0, 0, 0.95)) drop-shadow(-0.8px -0.8px 0.5px rgba(254, 240, 138, 0.65));
}
.card-tier-gold .silver-emv-chip {
  background: linear-gradient(135deg, #fef9c3 0%, #fef08a 25%, #eab308 60%, #ca8a04 85%, #854d0e 100%);
  border-color: #a16207;
}
.card-tier-gold .medallion-ring {
  background: linear-gradient(135deg, #fef08a 0%, #eab308 40%, #713f12 100%);
}
.card-tier-gold .medallion-vip-seal {
  background: linear-gradient(135deg, #facc15 0%, #ca8a04 100%);
  color: #1e1b06;
}

/* 4. THEME HẠNG TITAN (TITANIUM) - MÀU ĐEN TUYỀN (OBSIDIAN PITCH BLACK) */
.card-tier-titan,
.card-tier-platinum {
  background: radial-gradient(circle at 85% 15%, rgba(255, 255, 255, 0.08) 0%, transparent 40%),
              radial-gradient(circle at 15% 85%, rgba(45, 45, 55, 0.45) 0%, transparent 50%),
              linear-gradient(135deg, #050507 0%, #0d0d11 25%, #15151a 50%, #08080a 75%, #020203 100%);
  border: 1.5px solid rgba(255, 255, 255, 0.22);
  box-shadow: 0 28px 65px -12px rgba(0, 0, 0, 0.98),
              0 0 0 1px rgba(255, 255, 255, 0.08),
              inset 0 1px 2px rgba(255, 255, 255, 0.25),
              0 0 35px rgba(0, 0, 0, 0.9);
}
.card-tier-titan.is-clickable:hover,
.card-tier-platinum.is-clickable:hover {
  transform: translateY(-5px) scale(1.015);
  box-shadow: 0 36px 80px -12px rgba(0, 0, 0, 1),
              0 0 0 1.5px rgba(255, 255, 255, 0.4),
              0 0 30px rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.5);
}
.card-tier-titan .calligraphy-platinum-text,
.card-tier-platinum .calligraphy-platinum-text {
  background: linear-gradient(180deg, #ffffff 0%, #f4f4f5 22%, #d4d4d8 45%, #71717a 65%, #e4e4e7 85%, #3f3f46 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 1)) drop-shadow(0 0 18px rgba(255, 255, 255, 0.3));
}
.card-tier-titan .embossed-card-number,
.card-tier-platinum .embossed-card-number {
  background: linear-gradient(180deg, #ffffff 0%, #f4f4f5 35%, #a1a1aa 60%, #e4e4e7 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(1px 2px 2px rgba(0, 0, 0, 1)) drop-shadow(-0.8px -0.8px 0.5px rgba(255, 255, 255, 0.7));
}
.card-tier-titan .silver-emv-chip,
.card-tier-platinum .silver-emv-chip {
  background: linear-gradient(135deg, #27272a 0%, #18181b 30%, #3f3f46 60%, #09090b 85%, #27272a 100%);
  border-color: rgba(255, 255, 255, 0.3);
  box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.3), 0 3px 8px rgba(0, 0, 0, 0.9);
}
.card-tier-titan .silver-emv-chip .chip-slot,
.card-tier-platinum .silver-emv-chip .chip-slot {
  background: rgba(255, 255, 255, 0.22);
}
.card-tier-titan .silver-emv-chip .chip-center-chamber,
.card-tier-platinum .silver-emv-chip .chip-center-chamber {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.1);
}
.card-tier-titan .medallion-ring,
.card-tier-platinum .medallion-ring {
  background: linear-gradient(135deg, #ffffff 0%, #71717a 40%, #18181b 100%);
}
.card-tier-titan .medallion-vip-seal,
.card-tier-platinum .medallion-vip-seal {
  background: linear-gradient(135deg, #27272a 0%, #09090b 100%);
  border: 1px solid rgba(255, 255, 255, 0.4);
  color: #fff;
}

.vcb-platinum-card.is-clickable {
  cursor: pointer;
}

.vcb-platinum-card:focus-visible {
  outline: 2px solid var(--primary);
  outline-offset: 4px;
}

/* Ánh kim quét động */
.card-metallic-sheen {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: linear-gradient(
    115deg,
    transparent 35%,
    rgba(255, 255, 255, 0.05) 45%,
    rgba(255, 255, 255, 0.2) 50%,
    rgba(255, 255, 255, 0.05) 55%,
    transparent 65%
  );
  pointer-events: none;
  animation: cardSheenSweep 9s infinite linear;
}

.sheen-bronze {
  background: linear-gradient(115deg, transparent 35%, rgba(251, 146, 60, 0.08) 45%, rgba(254, 215, 170, 0.28) 50%, rgba(251, 146, 60, 0.08) 55%, transparent 65%);
}
.sheen-silver {
  background: linear-gradient(115deg, transparent 35%, rgba(255, 255, 255, 0.08) 45%, rgba(255, 255, 255, 0.35) 50%, rgba(255, 255, 255, 0.08) 55%, transparent 65%);
}
.sheen-gold {
  background: linear-gradient(115deg, transparent 35%, rgba(250, 204, 21, 0.1) 45%, rgba(254, 240, 138, 0.35) 50%, rgba(250, 204, 21, 0.1) 55%, transparent 65%);
}
.sheen-titan,
.sheen-platinum {
  background: linear-gradient(115deg, transparent 35%, rgba(255, 255, 255, 0.03) 45%, rgba(255, 255, 255, 0.22) 50%, rgba(255, 255, 255, 0.03) 55%, transparent 65%);
}

@keyframes cardSheenSweep {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

/* Overlay các khối đa giác kim loại */
.card-facets-overlay {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

/* Hàng trên: Brand Badge & Avatar */
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: relative;
  z-index: 2;
}

.header-left-space {
  display: flex;
  align-items: center;
}

.card-tier-pill-badge {
  font: 800 0.65rem/1 'Oswald', sans-serif;
  letter-spacing: 0.12em;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(4px);
}
.badge-bronze { color: #fed7aa; border-color: rgba(245, 158, 11, 0.4); }
.badge-silver { color: #f1f5f9; border-color: rgba(226, 232, 240, 0.6); background: rgba(30, 41, 59, 0.6); }
.badge-gold { color: #fef08a; border-color: rgba(250, 204, 21, 0.5); }
.badge-titan, .badge-platinum { color: #ffffff; border-color: rgba(255, 255, 255, 0.45); background: rgba(5, 5, 8, 0.9); box-shadow: 0 0 10px rgba(0, 0, 0, 0.8), inset 0 0 6px rgba(255, 255, 255, 0.1); }

.header-right-group {
  display: flex;
  align-items: center;
  gap: 14px;
}

/* Avatar hội viên */
.member-avatar-medallion {
  position: relative;
  display: flex;
  align-items: center;
}

.medallion-ring {
  width: 44px;
  height: 44px;
  border-radius: 50% !important;
  overflow: hidden;
  padding: 2.5px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.75), 0 0 0 1px rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
}

.medallion-img {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  object-fit: cover;
  display: block;
}

.medallion-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50% !important;
  background: #1e293b;
  color: #f8fafc;
  display: grid;
  place-items: center;
  font: 800 1.1rem 'Oswald', sans-serif;
}

.medallion-vip-seal {
  position: absolute;
  bottom: -3px;
  right: -4px;
  font: 900 0.52rem 'Oswald', sans-serif;
  letter-spacing: 0.05em;
  padding: 1.5px 5px;
  border-radius: 999px !important;
  border: 1px solid #fff;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.8);
}

/* Khu vực trung tâm: Chip EMV & Thư pháp */
.card-mid-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  position: relative;
  z-index: 2;
  margin: 2px 0 6px;
}

/* Chip EMV */
.silver-emv-chip {
  width: 54px;
  height: 40px;
  border-radius: 7px !important;
  border: 1px solid #475569;
  box-shadow: inset 0 1px 2px #fff, 0 3px 8px rgba(0, 0, 0, 0.7);
  position: relative;
  overflow: hidden;
  flex-shrink: 0;
}

.chip-slot {
  position: absolute;
  background: rgba(15, 23, 42, 0.45);
}

.slot-h1 { top: 34%; left: 0; right: 0; height: 1px; }
.slot-h2 { bottom: 34%; left: 0; right: 0; height: 1px; }
.slot-v1 { top: 0; bottom: 0; left: 34%; width: 1px; }
.slot-v2 { top: 0; bottom: 0; right: 34%; width: 1px; }

.chip-center-chamber {
  position: absolute;
  top: 24%;
  bottom: 24%;
  left: 34%;
  right: 34%;
  border: 1px solid rgba(15, 23, 42, 0.4);
  border-radius: 2px !important;
  background: rgba(255, 255, 255, 0.25);
}

/* Thư pháp uốn lượn trung tâm */
.calligraphy-platinum-wrap {
  flex: 1;
  text-align: center;
  margin-left: -20px;
}

.calligraphy-platinum-text {
  font-family: 'Great Vibes', 'Alex Brush', 'Playfair Display', 'Brush Script MT', cursive;
  font-size: clamp(2.8rem, 6vw, 4rem);
  font-style: italic;
  font-weight: 500;
  letter-spacing: 0.04em;
  display: inline-block;
  line-height: 1;
  transform: translateY(-2px);
}

/* Hàng dưới: Số thẻ & Metadata */
.card-footer-section {
  position: relative;
  z-index: 2;
}

/* Dãy số thẻ dập nổi 3D */
.embossed-card-number {
  font-family: 'Share Tech Mono', 'Farrington 7B', 'OCR A Extended', monospace;
  font-size: clamp(1.12rem, 2.25vw, 1.38rem);
  font-weight: 700;
  letter-spacing: 0.12em;
  margin-bottom: 6px;
  white-space: nowrap;
  width: 100%;
}

.card-metadata-grid {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.meta-col-cardholder {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-subline {
  display: flex;
  align-items: center;
  gap: 28px;
}

.prefix-embossed {
  font-family: monospace;
  font-size: 0.72rem;
  letter-spacing: 0.1em;
  color: #cbd5e1;
  font-weight: 700;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.9);
}

.valid-thru-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.vt-headers {
  display: flex;
  flex-direction: column;
  line-height: 1;
}

.vt-headers span {
  font-size: 0.44rem;
  letter-spacing: 0.1em;
  color: #94a3b8;
  font-weight: 700;
}

.vt-headers .vt-my {
  font-size: 0.38rem;
  color: #64748b;
  margin-top: 1px;
}

.vt-digits {
  font-family: monospace;
  font-size: 0.88rem;
  letter-spacing: 0.1em;
  color: #f8fafc;
  font-weight: 700;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.9);
}

.meta-owner-name {
  font-family: 'Share Tech Mono', 'Farrington 7B', sans-serif;
  font-size: 0.94rem;
  letter-spacing: 0.14em;
  font-weight: 700;
  color: #f8fafc;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.9), -0.5px -0.5px 0.5px rgba(255, 255, 255, 0.4);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 280px;
}

/* ==========================================================================
   MODAL CHI TIẾT THẺ HỘI VIÊN (INTERACTIVE PASSPORT MODAL)
   ========================================================================== */

.card-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.86);
  backdrop-filter: blur(8px);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  animation: modalFadeIn 0.25s ease-out;
}

.card-modal-container {
  background: #0f1217;
  color: #f8fafc;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 16px !important;
  width: 100%;
  max-width: 860px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 35px 80px -15px rgba(0, 0, 0, 0.95), 0 0 0 1px rgba(255, 255, 255, 0.08);
  overflow: hidden;
  animation: modalSlideUp 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.card-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 20px 26px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

.cm-tag {
  font: 700 0.68rem 'Oswald', sans-serif;
  letter-spacing: 0.15em;
  color: var(--accent, #f05a1a);
  display: block;
  margin-bottom: 4px;
}

.cm-title {
  margin: 0;
  font: 700 1.35rem 'Oswald', sans-serif;
  letter-spacing: 0.04em;
  color: #fff;
}

.cm-desc {
  margin: 4px 0 0;
  font-size: 0.85rem;
  color: #94a3b8;
}

.cm-close-btn {
  background: none;
  border: none;
  font-size: 1.8rem;
  color: #94a3b8;
  cursor: pointer;
  line-height: 1;
  padding: 0 4px;
}
.cm-close-btn:hover { color: #fff; }

.card-modal-body {
  padding: 22px 26px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* 3D Flip Card Stage */
.flip-card-stage {
  perspective: 1400px;
  width: 100%;
  max-width: 480px;
  margin: 0 auto;
}

.flip-card-box {
  width: 100%;
  aspect-ratio: 1.586 / 1;
  position: relative;
  transform-style: preserve-3d;
  transition: transform 0.75s cubic-bezier(0.4, 0.2, 0.2, 1);
}

.flip-card-box.is-flipped {
  transform: rotateY(180deg);
}

.flip-side {
  position: absolute;
  inset: 0;
  backface-visibility: hidden;
  border-radius: 18px;
}

.flip-front {
  z-index: 2;
  transform: rotateY(0deg);
}

.flip-back {
  transform: rotateY(180deg);
}

.preview-card-mode {
  min-height: unset;
  height: 100%;
}

/* Mặt sau của thẻ */
.card-back-container {
  width: 100%;
  height: 100%;
  background: #090b0e;
  border-radius: 18px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 24px 50px rgba(0, 0, 0, 0.9);
  padding: 20px 0 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}
.back-tier-bronze { background: linear-gradient(135deg, #180d06 0%, #2f170a 100%); border-color: rgba(245, 158, 11, 0.4); }
.back-tier-silver { background: linear-gradient(135deg, #1e2633 0%, #2b3545 100%); border-color: rgba(226, 232, 240, 0.6); }
.back-tier-gold { background: linear-gradient(135deg, #1c1402 0%, #302305 100%); border-color: rgba(234, 179, 8, 0.5); }
.back-tier-titan, .back-tier-platinum { background: linear-gradient(135deg, #050507 0%, #0d0d12 100%); border-color: rgba(255, 255, 255, 0.22); }

.magnetic-stripe {
  width: 100%;
  height: 44px;
  background: #000;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.8), 0 1px 1px rgba(255,255,255,0.06);
}

.signature-row {
  display: flex;
  align-items: center;
  padding: 0 22px;
  gap: 14px;
}

.signature-panel {
  flex: 1;
  height: 38px;
  background: repeating-linear-gradient(
    45deg,
    #e2e8f0,
    #e2e8f0 8px,
    #f8fafc 8px,
    #f8fafc 16px
  );
  border-radius: 4px;
  display: flex;
  align-items: center;
  padding: 0 10px;
  overflow: hidden;
}

.sig-pattern {
  font: 800 0.52rem 'Oswald', sans-serif;
  color: #64748b;
  letter-spacing: 0.15em;
  opacity: 0.7;
  white-space: nowrap;
}

.cvv-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
}

.cvv-label {
  font-size: 0.6rem;
  color: #94a3b8;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.cvv-value-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  color: #0f172a;
  padding: 3px 8px;
  border-radius: 4px;
}

.cvv-number {
  font-family: 'Share Tech Mono', monospace;
  font-size: 0.95rem;
  letter-spacing: 0.1em;
}

.cvv-toggle {
  background: none;
  border: none;
  font-size: 0.68rem;
  color: #475569;
  cursor: pointer;
  font-weight: 700;
  padding: 0;
}
.cvv-toggle:hover { color: #000; }

.back-meta-grid {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 22px;
}

.hologram-seal {
  width: 60px;
  height: 44px;
  border-radius: 6px;
  background: linear-gradient(135deg, #cbd5e1 0%, #64748b 30%, #e2e8f0 70%, #94a3b8 100%);
  position: relative;
  overflow: hidden;
  display: grid;
  place-items: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.6);
}
.holo-bronze { background: linear-gradient(135deg, #fed7aa 0%, #b45309 40%, #fde68a 70%, #78350f 100%); }
.holo-silver { background: linear-gradient(135deg, #f1f5f9 0%, #94a3b8 40%, #ffffff 70%, #64748b 100%); }
.holo-gold { background: linear-gradient(135deg, #fef08a 0%, #ca8a04 40%, #ffffff 70%, #854d0e 100%); }
.holo-titan, .holo-platinum { background: linear-gradient(135deg, #ffffff 0%, #52525b 35%, #a1a1aa 70%, #18181b 100%); }

.holo-shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4), transparent);
  animation: holoSweep 4s infinite linear;
}

@keyframes holoSweep {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.holo-text {
  font: 900 0.5rem/1.1 'Oswald', sans-serif;
  color: #0f172a;
  text-align: center;
  letter-spacing: 0.08em;
  position: relative;
  z-index: 1;
}

.showroom-qr-block {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.qr-box {
  background: #fff;
  color: #000;
  padding: 4px;
  border-radius: 6px;
  display: grid;
  place-items: center;
}

.qr-sub {
  font-size: 0.65rem;
  color: #94a3b8;
}

.back-footer-text {
  padding: 0 22px;
  font-size: 0.65rem;
  color: #64748b;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

/* Nút lật thẻ */
.flip-control-bar {
  display: flex;
  justify-content: center;
  margin-top: 14px;
}

.flip-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border-radius: 999px !important;
  padding: 6px 18px;
  font-size: 0.85rem;
}

/* Bảng thông tin chi tiết */
.card-details-info-section {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 12px;
  padding: 20px;
}

.info-section-title {
  margin: 0 0 14px;
  font: 700 0.95rem 'Oswald', sans-serif;
  letter-spacing: 0.08em;
  color: #e2e8f0;
}

.details-specs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}

.spec-card {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 10px 14px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.spec-lbl {
  font-size: 0.72rem;
  color: #94a3b8;
}

.spec-val-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.spec-val-mono {
  font-family: 'Share Tech Mono', monospace;
  font-size: 0.92rem;
  color: #f8fafc;
}

.btn-copy-action {
  font-size: 0.7rem;
  padding: 2px 8px;
  border-radius: 4px;
}

.spec-val-tier {
  font: 700 0.92rem 'Oswald', sans-serif;
  letter-spacing: 0.04em;
}
.tier-text-bronze { color: #f59e0b; }
.tier-text-silver { color: #cbd5e1; }
.tier-text-gold { color: #facc15; }
.tier-text-titan, .tier-text-platinum { color: #ffffff; text-shadow: 0 0 10px rgba(255, 255, 255, 0.4); }

.spec-val-name, .spec-val-valid {
  font-size: 0.92rem;
  color: #f8fafc;
  font-weight: 600;
}

/* Danh mục đặc quyền */
.perks-list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: 12px;
}

.perk-box {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  padding: 10px 12px;
}

.perk-badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-top: 5px;
  flex-shrink: 0;
}
.dot-bronze { background: #f59e0b; box-shadow: 0 0 8px rgba(245, 158, 11, 0.6); }
.dot-silver { background: #cbd5e1; box-shadow: 0 0 8px rgba(203, 213, 225, 0.6); }
.dot-gold { background: #facc15; box-shadow: 0 0 8px rgba(250, 204, 21, 0.6); }
.dot-titan, .dot-platinum { background: #ffffff; box-shadow: 0 0 10px rgba(255, 255, 255, 0.9); }

.perk-box b {
  display: block;
  font-size: 0.86rem;
  color: #f8fafc;
  margin-bottom: 2px;
}

.perk-box p {
  margin: 0;
  font-size: 0.76rem;
  color: #94a3b8;
  line-height: 1.35;
}

/* Modal Footer */
.card-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 26px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.02);
}

@keyframes modalFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes modalSlideUp {
  from { transform: translateY(20px) scale(0.97); opacity: 0; }
  to { transform: translateY(0) scale(1); opacity: 1; }
}

@media (max-width: 640px) {
  .vcb-platinum-card {
    min-height: 230px;
    padding: 16px 18px;
  }
  .calligraphy-platinum-text {
    font-size: 2.3rem;
  }
  .embossed-card-number {
    font-size: 1.02rem;
  }
  .meta-owner-name {
    font-size: 0.8rem;
  }
}
</style>
