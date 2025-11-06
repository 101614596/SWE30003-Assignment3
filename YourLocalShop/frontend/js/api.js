const API_BASE_URL = 'http://localhost:8080/api';

class API {
    // Helper method to handle fetch errors
    static async fetchWithErrorHandling(url, options = {}) {
        try {
            console.log('Fetching:', url, options.method || 'GET');
            const response = await fetch(url, options);

            console.log('Response status:', response.status);

            if (!response.ok) {
                const errorText = await response.text();
                console.error('Server error:', errorText);
                throw new Error(`Server error: ${response.status}`);
            }

            const data = await response.json();
            console.log('Response data:', data);
            return data;
        } catch (error) {
            console.error('Fetch error:', error);


            if (error.message.includes('Failed to fetch')) {
                throw new Error('Cannot connect to server. Make sure the backend is running on port 8080.');
            }
            throw error;
        }
    }

    // Products
    static async getAllProducts() {
        return await this.fetchWithErrorHandling(`${API_BASE_URL}/products`);
    }

    static async getProduct(id) {
        return await this.fetchWithErrorHandling(`${API_BASE_URL}/products/${id}`);
    }

    static async searchProducts(category) {
        return await this.fetchWithErrorHandling(`${API_BASE_URL}/products?category=${category}`);
    }

    // Cart
    static async addToCart(productId, quantity) {
        return await this.fetchWithErrorHandling(`${API_BASE_URL}/cart`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ productId, quantity })
        });
    }

    static async getCart() {
        return await this.fetchWithErrorHandling(`${API_BASE_URL}/cart`);
    }

    static async removeFromCart(productId) {
        return await this.fetchWithErrorHandling(`${API_BASE_URL}/cart/${productId}`, {
            method: 'DELETE',
            headers: {
                'Accept': 'application/json'
            }
        });
    }

    // Checkout
    static async checkout(customerData, paymentMethod) {
        return await this.fetchWithErrorHandling(`${API_BASE_URL}/orders`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            },
            body: JSON.stringify({ customerData, paymentMethod })
        });
    }
}